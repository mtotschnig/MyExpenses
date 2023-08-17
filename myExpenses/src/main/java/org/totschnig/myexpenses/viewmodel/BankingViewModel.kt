package org.totschnig.myexpenses.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.callback.AbstractHBCICallback
import org.kapott.hbci.exceptions.HBCI_Exception
import org.kapott.hbci.manager.BankInfo
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.HBCIVersion
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.configureAttributes
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.createBank
import org.totschnig.myexpenses.db2.deleteBank
import org.totschnig.myexpenses.db2.importedAccounts
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.loadBank
import org.totschnig.myexpenses.db2.loadBanks
import org.totschnig.myexpenses.db2.saveAttributes
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_SYNCED_WITH_BANK
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.BankingCredentials
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Properties


/**
 * see [VerwendungszweckUtil.Tag]
 */
enum class FinTsAttribute(override val userVisible: Boolean = true) : Attribute {
    EREF,
    KREF,
    MREF,
    CRED,
    DBET,
    SALDO,
    CHECKSUM(false)
    ;

    companion object {
        const val CONTEXT = "FinTS"
    }

    override val context: String
        get() = CONTEXT


}

class BankingViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    init {
        System.setProperty(
            "javax.xml.parsers.DocumentBuilderFactory",
            "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"
        )
    }

    private val hbciProperties = Properties().also {
        it["client.product.name"] = "02F84CA8EC793B72255C747B4"
        if (BuildConfig.DEBUG) {
            it["log.loglevel.default"] = HBCIUtils.LOG_INTERN.toString()
        }
    }

    private val tanFuture: CompletableDeferred<String?> = CompletableDeferred()

    private val _tanRequested = MutableLiveData(false)

    val tanRequested: LiveData<Boolean> = _tanRequested

    private val _workState: MutableStateFlow<WorkState> =
        MutableStateFlow(WorkState.Initial)
    val workState: StateFlow<WorkState> = _workState

    private val _errorState: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val errorState: StateFlow<String?> = _errorState

    val converter: HbciConverter
        get() = HbciConverter(repository, currencyContext.get("EUR"))

    sealed class WorkState {

        object Initial : WorkState()

        data class Loading(val message: String) : WorkState()

        data class BankLoaded(val bank: Bank): WorkState()

        data class AccountsLoaded(
            /*
                pair of rowId in Database and bank name
             */
            val bank: Pair<Long, String>,
            /*
                Konto to Boolean that indicates if the account has already been imported
             */
            val accounts: List<Pair<Konto, Boolean>>
        ) : WorkState()

        data class Done(val message: String = "") : WorkState()
    }

    fun submitTan(tan: String?) {
        tanFuture.complete(tan)
        _tanRequested.postValue(false)
    }

    private fun log(msg: String) {
        Timber.tag("FinTS").i(msg)
    }

    private fun error(msg: String) {
        _errorState.value = msg
    }

    private fun error(exception: Exception) {
        error(Utils.getCause(exception).safeMessage)
    }

    //TODO this will be moved into featureManager
    fun initAttributes() {
        viewModelScope.launch(context = coroutineContext()) {
            repository.configureAttributes(FinTsAttribute.values().asList())
        }
    }

    private fun initHBCI(bankingCredentials: BankingCredentials): BankInfo? {
        HBCIUtils.init(hbciProperties, MyHBCICallback(bankingCredentials))
        HBCIUtils.setParam("client.passport.default", "PinTan")
        HBCIUtils.setParam("client.passport.PinTan.init", "1")

        val info = HBCIUtils.getBankInfo(bankingCredentials.blz)

        if (info == null) {
            HBCIUtils.doneThread()
            error("${bankingCredentials.blz} not found in the list of banks that support FinTS")
            _workState.value = WorkState.Initial
        }
        return info
    }

    private fun buildPassportFile(info: BankInfo, user: String) =
        File(
            getApplication<MyApplication>().filesDir,
            "testpassport_${info.blz}_${user}.dat"
        )

    private fun buildPassport(info: BankInfo, file: File) =
        AbstractHBCIPassport.getInstance(file).apply {
            country = "DE"
            host = info.pinTanAddress
            port = 443
            filterType = "Base64"
        }

    @WorkerThread
    private fun doHBCI(
        bankingCredentials: BankingCredentials,
        work: (BankInfo, HBCIPassport, HBCIHandler) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val info = initHBCI(bankingCredentials) ?: return

        val passportFile = buildPassportFile(info, bankingCredentials.user)

        val passport = try {
            buildPassport(info, passportFile)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e)
            }
            HBCIUtils.doneThread()
            onError(Exception("Wrong PIN"))
            return
        }

        val handle = try {
            HBCIHandler(HBCIVersion.HBCI_300.id, passport)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e)
            }
            passport.close()
            passportFile.delete()
            HBCIUtils.doneThread()
            onError(e)
            return
        }
        try {
            work(info, passport, handle)
        } finally {
            handle.close()
            passport.close()
            HBCIUtils.doneThread()
        }
    }

    fun loadBank(bankId: Long) {
        viewModelScope.launch(context = coroutineContext()) {
            _workState.value = WorkState.BankLoaded(repository.loadBank(bankId))
        }
    }

    fun addBank(bankingCredentials: BankingCredentials) {
        clearError()
        _workState.value = WorkState.Loading("Loading information")

        if (bankingCredentials.isNew && banks.value.any { it.blz == bankingCredentials.blz && it.userId == bankingCredentials.user }) {
            _workState.value = WorkState.Initial
            error("Bank has already been added")
            return
        }
        viewModelScope.launch(context = coroutineContext()) {
            doHBCI(
                bankingCredentials,
                work = { info, passport, _ ->
                    val bank = if (bankingCredentials.isNew) {
                        with(
                            repository.createBank(
                                Bank(
                                    blz = info.blz,
                                    bic = info.bic,
                                    bankName = info.name,
                                    userId = bankingCredentials.user
                                )
                            )
                        ) { id to bankName }
                    } else bankingCredentials.bank!!

                    val importedAccounts = bankingCredentials.bank?.let {
                        repository.importedAccounts(it.first)
                    }

                    val accounts = passport.accounts
                        ?.map { it to (importedAccounts?.contains(it.dbNumber) == true) }
                    if (accounts.isNullOrEmpty()) {
                        error("Keine Konten ermittelbar")
                    } else {
                        _workState.value = WorkState.AccountsLoaded(bank, accounts)
                    }
                },
                onError = {
                    error(it)
                    _workState.value = WorkState.Initial
                }
            )
        }
    }

    fun syncAccount(
        credentials: BankingCredentials,
        accountId: Long
    ) {
        viewModelScope.launch(context = coroutineContext()) {
            _workState.value = WorkState.Loading("Loading account information")
            val account = repository.loadAccount(accountId)!!
            doHBCI(
                credentials,
                work = { _, _, handle ->

                    _workState.value = WorkState.Loading("Syncing account")

                    val umsatzJob: HBCIJob = handle.newJob("KUmsAll")
                    umsatzJob.setParam("my.blz", credentials.blz)
                    umsatzJob.setParam("my.number", account.accountNumber)
                    umsatzJob.setStartParam(account.lastSyncedWithBank!!)
                    umsatzJob.addToQueue()

                    val status: HBCIExecStatus = handle.execute()

                    if (!status.isOK) {
                        error(status.toString())
                        _workState.value = WorkState.Done()
                        return@doHBCI
                    }

                    val result = umsatzJob.jobResult as GVRKUms

                    if (!result.isOK) {
                        error(result.toString())
                        _workState.value = WorkState.Done()
                        return@doHBCI
                    }

                    var importCount = 0
                    for (umsLine in result.flatData) {
                        log(umsLine.toString())
                        with(converter) {
                            val (transaction, attributes: Map<out Attribute, String>) =
                                umsLine.toTransaction(accountId)
                            if (isDuplicate(transaction, attributes[FinTsAttribute.CHECKSUM]!!)) {
                                Timber.d("Found duplicatee for $umsLine")
                            } else {
                                val id = ContentUris.parseId(transaction.save()!!)
                                repository.saveAttributes(id, attributes)

                                importCount++
                            }
                        }
                    }
                    repository.updateAccount(
                        accountId,
                        ContentValues().apply {
                            put(
                                KEY_LAST_SYNCED_WITH_BANK,
                                LocalDate.now().toString()
                            )
                        })
                    _workState.value = WorkState.Done(if (importCount > 0) "$importCount transactions imported" else "No new transactions")
                },
                onError = {
                    error(it)
                    _workState.value = WorkState.Done()
                }
            )
        }
    }

    @SuppressLint("Recycle")
    private fun isDuplicate(transaction: Transaction, checkSum: String): Boolean {
        return contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_AMOUNT, KEY_DATE),
        "(select $KEY_VALUE from $TABLE_TRANSACTION_ATTRIBUTES left join $TABLE_ATTRIBUTES on $KEY_ATTRIBUTE_ID = $TABLE_ATTRIBUTES.$KEY_ROWID WHERE $KEY_ATTRIBUTE_NAME = ? and $KEY_TRANSACTIONID = $VIEW_COMMITTED.$KEY_ROWID) = ? ",
            arrayOf(FinTsAttribute.CHECKSUM.name, checkSum),null
        )?.useAndMap {
            it.getLong(0) == transaction.amount.amountMinor && it.getLong(1) == transaction.date
        }?.any { it } == true
    }

    private fun HBCIJob.setStartParam(localDate: LocalDate) {
        setParam(
            "startdate",
            Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        )
    }

    fun importAccounts(
        bankingCredentials: BankingCredentials,
        bank: Pair<Long, String>,
        accounts: List<Konto>,
        startDate: LocalDate?
    ) {
        clearError()
        var successCount = 0
        viewModelScope.launch(context = coroutineContext()) {
            accounts.forEach {

                doHBCI(
                    bankingCredentials,
                    work = { _, _, handle ->

                        _workState.value = WorkState.Loading("Importing account ${it.iban}")

                        val umsatzJob: HBCIJob = handle.newJob("KUmsAll")
                        log("jobRestrictions : " + umsatzJob.jobRestrictions.toString())
                        umsatzJob.setParam("my", it)
                        startDate?.let { umsatzJob.setStartParam(startDate) }

                        umsatzJob.addToQueue()

                        val status: HBCIExecStatus = handle.execute()

                        if (!status.isOK) {
                            error(status.toString())
                            return@doHBCI
                        }

                        val result = umsatzJob.jobResult as GVRKUms

                        if (!result.isOK) {
                            error(result.toString())
                            return@doHBCI
                        }

                        val dbAccount = repository.createAccount(
                            it.toAccount(bank, result.dataPerDay.firstOrNull()?.start?.value?.longValue ?: 0L)
                        )

                        log("created account in db with id ${dbAccount.id}")
                        for (umsLine in result.flatData) {
                            log(umsLine.toString())
                            with(converter) {
                                val (transaction, attributes: Map<out Attribute, String>) = umsLine.toTransaction(
                                    dbAccount.id
                                )
                                val id = ContentUris.parseId(transaction.save()!!)
                                repository.saveAttributes(id, attributes)
                            }
                        }
                        repository.updateAccount(dbAccount.id, ContentValues().apply { put(KEY_LAST_SYNCED_WITH_BANK, LocalDate.now().toString()) })
                        successCount++
                    },
                    onError = {
                        error(it)
                        _workState.value = WorkState.Done()
                    }
                )
            }
            _workState.value = WorkState.Done("$successCount accounts successfully imported.")
        }
    }

    fun deleteBank(id: Long) {
        repository.deleteBank(id)
    }

    fun reset() {
        _workState.value = WorkState.Initial
        clearError()
    }

    private fun clearError() {
        _errorState.value = null
    }


    inner class MyHBCICallback(private val bankingCredentials: BankingCredentials) :
        AbstractHBCICallback() {
        override fun log(msg: String, level: Int, date: Date, trace: StackTraceElement) {
            Timber.tag("FinTS").d(msg)
        }

        override fun callback(
            passport: HBCIPassport?,
            reason: Int,
            msg: String,
            datatype: Int,
            retData: StringBuffer
        ) {
            Timber.tag("FinTS").i("callback:%d", reason)
            when (reason) {
                NEED_PASSPHRASE_LOAD, NEED_PASSPHRASE_SAVE -> {
                    retData.replace(0, retData.length, bankingCredentials.password!!)
                }

                NEED_PT_PIN -> retData.replace(0, retData.length, bankingCredentials.password!!)
                NEED_BLZ -> retData.replace(0, retData.length, bankingCredentials.bankLeitZahl)
                NEED_USERID -> retData.replace(0, retData.length, bankingCredentials.user)
                NEED_CUSTOMERID -> retData.replace(0, retData.length, bankingCredentials.user)
                NEED_PT_PHOTOTAN ->
                    try {
                        TODO()
                    } catch (e: Exception) {
                        throw HBCI_Exception(e)
                    }

                NEED_PT_QRTAN ->
                    try {
                        TODO()
                    } catch (e: Exception) {
                        throw HBCI_Exception(e)
                    }

                NEED_PT_SECMECH -> {
                    val options = retData.toString().split("|")
                    if (options.size > 1) {
                        Timber.e(
                            "SecMech Selection Dialog not yet implemented ().",
                            retData.toString()
                        )
                    }
                    val firstOption = options[0]
                    retData.replace(
                        0,
                        retData.length,
                        firstOption.substring(0, firstOption.indexOf(":"))
                    )
                }

                NEED_PT_TAN -> {
                    val flicker = retData.toString()
                    if (flicker.isNotEmpty()) {
                        TODO()
                    } else {
                        _tanRequested.postValue(true)
                        retData.replace(0, retData.length, runBlocking {
                            val result =
                                tanFuture.await() ?: throw HBCI_Exception("TAN entry cancelled")
                            result
                        })
                    }
                }

                NEED_PT_TANMEDIA -> {}
                HAVE_ERROR -> Timber.d(msg)
                else -> {}
            }
        }

        override fun status(passport: HBCIPassport, statusTag: Int, o: Array<Any>?) {
            Timber.tag("FinTS").i("status:%d", statusTag)
            o?.forEach {
                Timber.tag("FinTS").i(it.toString())
            }
        }
    }

    val banks: StateFlow<List<Bank>> by lazy {
        repository.loadBanks().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
    }
}