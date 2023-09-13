package org.totschnig.fints

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.kapott.hbci.manager.MatrixCode
import org.kapott.hbci.manager.QRCode
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Konto
import org.totschnig.fints.R
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.BankingAttribute
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.db2.accountInformation
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.createBank
import org.totschnig.myexpenses.db2.deleteBank
import org.totschnig.myexpenses.db2.importedAccounts
import org.totschnig.myexpenses.db2.loadBank
import org.totschnig.myexpenses.db2.loadBanks
import org.totschnig.myexpenses.db2.saveAccountAttributes
import org.totschnig.myexpenses.db2.saveTransactionAttributes
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel
import timber.log.Timber
import java.io.File
import java.io.StreamCorruptedException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Properties
import org.totschnig.fints.R as RF

data class TanRequest(val message: String, val bitmap: Bitmap?)

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

    private val _tanRequested = MutableLiveData<TanRequest?>(null)

    val tanRequested: LiveData<TanRequest?> = _tanRequested

    private val _workState: MutableStateFlow<WorkState> =
        MutableStateFlow(WorkState.Initial)
    val workState: StateFlow<WorkState> = _workState

    private val _errorState: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val errorState: StateFlow<String?> = _errorState

    private val converter: HbciConverter
        get() = HbciConverter(repository, currencyContext.get("EUR"))

    sealed class WorkState {

        data object Initial : WorkState()

        data class Loading(val message: String? = null) : WorkState()

        data class BankLoaded(val bank: Bank) : WorkState()

        data class AccountsLoaded(
            val bank: Bank,
            /*
                Konto to Boolean that indicates if the account has already been imported
             */
            val accounts: List<Pair<Konto, Boolean>>
        ) : WorkState()

        abstract class Done() : WorkState()

        class Abort: Done()

        class Success(val message: String = ""): Done()
    }

    fun submitTan(tan: String?) {
        tanFuture.complete(tan)
        _tanRequested.postValue(null)
    }

    private fun log(msg: String) {
        Timber.tag(BankingFeature.TAG).i(msg)
    }

    private fun error(msg: String) {
        _errorState.value = msg
    }

    private fun error(exception: Exception) {
        error(Utils.getCause(exception).safeMessage)
    }

    private fun initHBCI(bankingCredentials: BankingCredentials): BankInfo? {
        HBCIUtils.init(hbciProperties, MyHBCICallback(bankingCredentials))
        HBCIUtils.setParam("client.passport.default", "PinTan")
        HBCIUtils.setParam("client.passport.PinTan.init", "1")

        return HBCIUtils.getBankInfo(bankingCredentials.blz)
    }

    private fun buildPassportFile(info: BankInfo, user: String) =
        File(
            getApplication<MyApplication>().filesDir,
            "passport_${info.blz}_${user}.dat"
        )

    private fun buildPassport(info: BankInfo, file: File) =
        AbstractHBCIPassport.getInstance(file).apply {
            country = "DE"
            host = info.pinTanAddress
            port = 443
            filterType = "Base64"
        }

    @WorkerThread
    private suspend fun doHBCI(
        bankingCredentials: BankingCredentials,
        work: suspend (BankInfo, HBCIPassport, HBCIHandler) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val info = initHBCI(bankingCredentials) ?: run {
            HBCIUtils.doneThread()
            onError(Exception(getString(R.string.blz_not_found, bankingCredentials.blz)))
            return
        }

        val passportFile = buildPassportFile(info, bankingCredentials.user)

        val passport = try {
            buildPassport(info, passportFile)
        } catch (e: Exception) {
            val exception = if (Utils.getCause(e) is StreamCorruptedException) {
                Exception(getString(R.string.wrong_pin))
            } else e
            HBCIUtils.doneThread()
            onError(exception)
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
        } catch (e: Exception) {
            Timber.e(e)
            onError(e)
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

        if (bankingCredentials.isNew && banks.value.any { it.blz == bankingCredentials.blz && it.userId == bankingCredentials.user }) {
            error(getString(R.string.bank_already_added))
            return
        }
        _workState.value = WorkState.Loading()
        viewModelScope.launch(context = coroutineContext()) {
            doHBCI(
                bankingCredentials,
                work = { info, passport, _ ->
                    val bank = if (bankingCredentials.isNew)
                        repository.createBank(
                            Bank(
                                blz = info.blz,
                                bic = info.bic,
                                bankName = info.name,
                                userId = bankingCredentials.user
                            )
                        ) else bankingCredentials.bank!!

                    val importedAccounts = bankingCredentials.bank?.let {
                        repository.importedAccounts(it.id)
                    }

                    val accounts = passport.accounts
                        ?.map { konto ->
                            konto to (importedAccounts?.any {
                                it.iban == konto.iban || (it.number == konto.number && it.subnumber == konto.subnumber)
                            } == true)
                        }
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
            _workState.value = WorkState.Loading()
            val accountInformation = repository.accountInformation(accountId)
            if (accountInformation == null) {
                CrashHandler.report(Exception("Error while retrieving Information for account"))
                error("Error while retrieving Information for account")
                _workState.value = WorkState.Abort()
                return@launch
            }
            if (accountInformation.lastSynced == null) {
                CrashHandler.report(Exception("Error while retrieving Information for account (lastSynced)"))
                error("Error while retrieving Information for account (lastSynced")
                _workState.value = WorkState.Abort()
                return@launch
            }

            doHBCI(
                credentials,
                work = { _, _, handle ->

                    _workState.value = WorkState.Loading()

                    val umsatzJob: HBCIJob = handle.newJob("KUmsAll")
                    umsatzJob.setParam("my",
                        Konto(
                            "DE",
                            credentials.blz,
                            accountInformation.number,
                            accountInformation.subnumber
                        ).also {
                            it.iban = accountInformation.iban
                            it.bic = credentials.bank!!.bic
                        }
                    )
                    umsatzJob.setStartParam(accountInformation.lastSynced!!)
                    umsatzJob.addToQueue()

                    val status: HBCIExecStatus = handle.execute()

                    if (!status.isOK) {
                        status.toString()
                        CrashHandler.report(Exception("Status was not ok"))
                    }

                    val result = umsatzJob.jobResult as GVRKUms

                    if (!result.isOK) {
                        error(result.toString())
                        log(result.toString())
                        _workState.value = WorkState.Abort()
                        return@doHBCI
                    }

                    var importCount = 0
                    for (umsLine in result.flatData) {
                        log(umsLine.toString())
                        with(converter) {
                            val (transaction, attributes: Map<out Attribute, String>) =
                                umsLine.toTransaction(accountId, currencyContext)
                            if (isDuplicate(transaction, attributes[FinTsAttribute.CHECKSUM]!!)) {
                                Timber.d("Found duplicate for $umsLine")
                            } else {
                                val id = ContentUris.parseId(transaction.save(contentResolver)!!)
                                repository.saveTransactionAttributes(id, attributes)

                                importCount++
                            }
                        }
                    }
                    setAccountLastSynced(accountId)
                    _workState.value =
                        WorkState.Success(
                            if (importCount > 0)
                                getQuantityString(R.plurals.transactions_imported, importCount, importCount)
                            else
                                getString(R.string.transactions_imported_none)
                        )
                },
                onError = {
                    error(it)
                    _workState.value = WorkState.Abort()
                }
            )
        }
    }

    private fun setAccountLastSynced(accountId: Long) {
        repository.saveAccountAttributes(
            accountId, listOf(
                BankingAttribute.LAST_SYCNED_WITH_BANK to LocalDate.now().toString()
            ).toMap()
        )
    }

    @SuppressLint("Recycle")
    private fun isDuplicate(transaction: Transaction, checkSum: String): Boolean {
        return contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_AMOUNT, KEY_DATE),
            "(select $KEY_VALUE from $TABLE_TRANSACTION_ATTRIBUTES left join $TABLE_ATTRIBUTES on $KEY_ATTRIBUTE_ID = $TABLE_ATTRIBUTES.$KEY_ROWID WHERE $KEY_ATTRIBUTE_NAME = ? and $KEY_TRANSACTIONID = $VIEW_COMMITTED.$KEY_ROWID) = ? ",
            arrayOf(FinTsAttribute.CHECKSUM.name, checkSum), null
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
        bank: Bank,
        accounts: List<Pair<Konto, Long>>,
        startDate: LocalDate?
    ) {
        clearError()
        var successCount = 0
        viewModelScope.launch(context = coroutineContext()) {
            accounts.forEach { (konto, targetAccount) ->

                doHBCI(
                    bankingCredentials,
                    work = { _, _, handle ->

                        _workState.value = WorkState.Loading(getString(RF.string.progress_importing_account, konto.iban))

                        val umsatzJob: HBCIJob = handle.newJob("KUmsAll")
                        log("jobRestrictions : " + umsatzJob.jobRestrictions.toString())
                        umsatzJob.setParam("my", konto)
                        startDate?.let { umsatzJob.setStartParam(startDate) }

                        umsatzJob.addToQueue()

                        val status: HBCIExecStatus = handle.execute()

                        if (!status.isOK) {
                            CrashHandler.report(Exception("Status was not ok"))
                        }

                        val result = umsatzJob.jobResult as GVRKUms

                        if (!result.isOK) {
                            _workState.value = WorkState.Abort()
                            error(result.toString())
                            log(result.toString())
                            return@doHBCI
                        }

                        val accountId = targetAccount.takeIf { it != 0L }?.also {
                            repository.updateAccount(it) {
                                put(KEY_BANK_ID, bank.id)
                            }
                        } ?:
                            repository.createAccount(
                                konto.toAccount(
                                    bank,
                                    result.dataPerDay.firstOrNull()?.start?.value?.longValue ?: 0L
                                )
                            ).id

                        repository.saveAccountAttributes(accountId, konto.asAttributes)

                        for (umsLine in result.flatData) {
                            log(umsLine.toString())
                            with(converter) {
                                val (transaction, transactionAttributes: Map<out Attribute, String>) = umsLine.toTransaction(
                                    accountId,
                                    currencyContext
                                )
                                val id = ContentUris.parseId(transaction.save(contentResolver)!!)
                                repository.saveTransactionAttributes(id, transactionAttributes)
                            }
                        }
                        setAccountLastSynced(accountId)
                        successCount++
                    },
                    onError = {
                        error(it)
                        _workState.value = WorkState.Abort()
                    }
                )
            }
            licenceHandler.recordUsage(ContribFeature.BANKING)
            _workState.value = WorkState.Success(
                getQuantityString(R.plurals.accounts_imported, successCount, successCount)
            )
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
            log(msg)
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
                        val code = MatrixCode(retData.toString())

                        val bitmap = BitmapFactory.decodeByteArray(code.image, 0, code.image.size)

                        _tanRequested.postValue(TanRequest(msg, bitmap))

                    } catch (e: Exception) {
                        throw HBCI_Exception(e)
                    }

                NEED_PT_QRTAN ->
                    try {
                        val code = QRCode(retData.toString(), msg)

                        val bitmap = BitmapFactory.decodeByteArray(code.image, 0, code.image.size)

                        _tanRequested.postValue(TanRequest(code.message, bitmap))

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
                        _tanRequested.postValue(TanRequest(msg,null))
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

    val accounts by lazy {
        accountsMinimal(
            query = "${DatabaseConstants.KEY_TYPE} != '${AccountType.CASH.name}' AND $KEY_BANK_ID IS NULL",
            withAggregates = false
        )
    }

}