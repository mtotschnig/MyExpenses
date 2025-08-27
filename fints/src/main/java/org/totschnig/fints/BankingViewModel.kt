package org.totschnig.fints

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.text.RandomStringGenerator
import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.callback.AbstractHBCICallback
import org.kapott.hbci.exceptions.HBCI_Exception
import org.kapott.hbci.manager.BankInfo
import org.kapott.hbci.manager.Feature
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.MatrixCode
import org.kapott.hbci.manager.QRCode
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Konto
import org.totschnig.fints.R
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.AccountInformation
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.BankingAttribute
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.db2.accountInformation
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.createBank
import org.totschnig.myexpenses.db2.deleteBank
import org.totschnig.myexpenses.db2.findAccountType
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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VERSION
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTION_ATTRIBUTES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.config.Configurator
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.crypt.PassphraseRepository
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel
import timber.log.Timber
import java.io.File
import java.io.StreamCorruptedException
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Properties
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.totschnig.fints.R as RF

data class TanRequest(val message: String, val bitmap: Bitmap?, val submit: (String?) -> Unit)
data class TanMediumRequest(val options: List<String>, val submit: (Pair<String, Boolean>?) -> Unit)
data class PushTanRequest(val message: String, val submit: () -> Unit)
data class SecMechRequest(val options: List<SecMech>, val submit: (Pair<String, Boolean>?) -> Unit)

data class SecMech(val id: String, val name: String) {
    companion object {
        fun parse(input: String) = input.split("|").map { option ->
            option.split(':').let {
                SecMech(it[0], it[1])
            }
        }
    }
}

class BankingViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    @Inject
    lateinit var tracker: Tracker
    @Inject
    lateinit var configurator: Configurator

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

    private fun keySelectedTanMedium(bankId: Long) = "selectedTanMedium_$bankId"
    private fun keySelectedSecMech(bankId: Long) = "selectedSecMech_$bankId"

    fun selectedTanMedium(bankId: Long) =
        prefHandler.getString(keySelectedTanMedium(bankId))

    fun selectedSecMech(bankId: Long) =
        prefHandler.getString(keySelectedSecMech(bankId))

    fun hasStoredTanMech(bankId: Long): Boolean =
        selectedSecMech(bankId) != null || selectedTanMedium(bankId) != null

    fun resetTanMechanism(bankId: Long) {
        prefHandler.remove(keySelectedSecMech(bankId))
        prefHandler.remove(keySelectedTanMedium(bankId))
    }

    private fun persistSelectedTanMedium(bankId: Long, tanMedium: String) {
        prefHandler.putString(keySelectedTanMedium(bankId), tanMedium)
    }

    private fun persistSelectedSecMech(bankId: Long, secMech: String) {
        prefHandler.putString(keySelectedSecMech(bankId), secMech)
    }


    private val _tanRequested = MutableLiveData<TanRequest?>(null)
    val tanRequested: LiveData<TanRequest?> = _tanRequested

    private fun requestTan(message: String, bitmap: Bitmap?): CompletableDeferred<String?> {
        val future = CompletableDeferred<String?>()
        _tanRequested.postValue(TanRequest(message, bitmap) {
            future.complete(it)
            _tanRequested.postValue(null)
        })
        return future
    }

    private val _tanMediumRequested = MutableLiveData<TanMediumRequest?>(null)
    val tanMediumRequested: LiveData<TanMediumRequest?> = _tanMediumRequested

    private fun requestTanMedium(options: List<String>): CompletableDeferred<Pair<String, Boolean>?> {
        val future = CompletableDeferred<Pair<String, Boolean>?>()
        _tanMediumRequested.postValue(TanMediumRequest(options) {
            future.complete(it)
            _tanMediumRequested.postValue(null)
        })
        return future
    }

    private val _pushTanRequested = MutableLiveData<PushTanRequest?>(null)
    val pushTanRequested: LiveData<PushTanRequest?> = _pushTanRequested

    private fun requestPushTan(message: String): CompletableDeferred<Unit> {
        val future = CompletableDeferred<Unit>()
        _pushTanRequested.postValue(PushTanRequest(message) {
            future.complete(Unit)
            _pushTanRequested.postValue(null)
        })
        return future
    }

    private val _secMechRequested = MutableLiveData<SecMechRequest?>(null)
    val secMechRequested: LiveData<SecMechRequest?> = _secMechRequested

    private fun requestSecMech(options: List<SecMech>): CompletableDeferred<Pair<String, Boolean>?> {
        val future = CompletableDeferred<Pair<String, Boolean>?>()
        _secMechRequested.postValue(SecMechRequest(options) {
            future.complete(it)
            _secMechRequested.postValue(null)
        })
        return future
    }

    private val _instMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val instMessage: StateFlow<String?> = _instMessage

    fun messageShown() {
        _instMessage.update { null }
    }

    private val _workState: MutableStateFlow<WorkState> = MutableStateFlow(WorkState.Initial)
    val workState: StateFlow<WorkState> = _workState

    private val _errorState: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorState: StateFlow<String?> = _errorState

    private val converter: HbciConverter
        get() = HbciConverter(repository, currencyContext["EUR"])

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

        abstract class Done : WorkState()

        data object Abort : Done()

        class Success(val message: String = "") : Done()
    }

    private fun logTree() = Timber.tag(BankingFeature.TAG)

    private fun log(msg: String) {
        logTree().i(msg)
    }

    private fun log(exception: Exception) {
        logTree().w(exception)
    }

    private fun error(msg: String) {
        _errorState.value = msg
    }

    private fun error(exception: Exception, bankingCredentials: BankingCredentials) {
        logEvent(Tracker.EVENT_FINTS_ERROR, bankingCredentials)
        log(exception)
        error(Utils.getCause(exception).safeMessage)
    }

    private fun logEvent(event: String, bankingCredentials: BankingCredentials) {
        tracker.logEvent(event, Bundle(1).apply {
            putString(
                Tracker.EVENT_PARAM_BLZ,
                bankingCredentials.bank?.blz ?: bankingCredentials.blz
            )
        })
    }

    private fun initHBCI(bankingCredentials: BankingCredentials): BankInfo? {
        HBCIUtils.init(hbciProperties, MyHBCICallback(bankingCredentials))
        HBCIUtils.setParam("client.passport.default", "PinTan")
        HBCIUtils.setParam("client.passport.PinTan.init", "1")
        Feature.INIT_FLIP_USER_INST.isEnabled = false
        return HBCIUtils.getBankInfo(bankingCredentials.blz)
            ?.takeIf { it.pinTanAddress != null }
    }

    private fun passportFile(blz: String, user: String) =
        File(
            getApplication<MyApplication>().filesDir,
            "passport_${blz}_${user}.dat"
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
        forceNewFile: Boolean = false,
        onError: (Exception) -> Unit
    ) {
        val info = initHBCI(bankingCredentials) ?: run {
            HBCIUtils.doneThread()
            onError(Exception(getString(R.string.blz_not_found, bankingCredentials.blz)))
            return
        }

        val passportFile = passportFile(info.blz, bankingCredentials.user).also {
            if (forceNewFile && it.exists()) {
                it.delete()
            }
        }

        val passport = try {
            buildPassport(info, passportFile)
        } catch (e: Exception) {
            log(e)
            HBCIUtils.doneThread()
            onError(
                if (Utils.getCause(e) is StreamCorruptedException) {
                    Exception(getString(R.string.wrong_pin))
                } else e
            )
            return
        }

        val handle = try {
            HBCIHandler(bankingCredentials.hbciVersion.id, passport)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e)
            }
            passport.close()
            HBCIUtils.doneThread()
            onError(e)
            return
        }
        try {
            work(info, passport, handle)
        } catch (e: Exception) {
            report(e)
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
        if (_workState.value is WorkState.Loading) {
            log("Double click")
            return
        }
        _workState.value = WorkState.Loading()
        viewModelScope.launch(context = coroutineContext()) {
            doHBCI(
                bankingCredentials,
                forceNewFile = bankingCredentials.isNew,
                work = { info, passport, _ ->
                    val bank = if (bankingCredentials.isNew) {
                        logEvent(Tracker.EVENT_FINTS_BANK_ADDED, bankingCredentials)
                        repository.createBank(
                            Bank(
                                blz = info.blz,
                                bic = info.bic,
                                bankName = info.name,
                                userId = bankingCredentials.user
                            )
                        )
                    } else bankingCredentials.bank!!

                    val importedAccounts = bankingCredentials.bank?.let {
                        repository.importedAccounts(it.id)
                    }

                    val accounts = passport.accounts
                        ?.map { konto ->
                            konto.also {
                                if (it.bic == null) {
                                    it.bic = info.bic
                                }
                            } to (importedAccounts?.any {
                                it.iban == konto.iban || (it.number == konto.number && it.subnumber == konto.subnumber)
                            } == true)
                        }
                    if (accounts.isNullOrEmpty()) {
                        error("Keine Konten ermittelbar")
                        _workState.value = WorkState.Abort
                    } else {
                        _workState.value = WorkState.AccountsLoaded(bank, accounts)
                    }
                },
                onError = {
                    error(it, bankingCredentials)
                    _workState.value = WorkState.Initial
                }
            )
        }
    }

    fun syncAccount(
        credentials: BankingCredentials,
        account: Pair<Long, Long>?,
    ) {
        if (_workState.value is WorkState.Loading) {
            log("Double click")
            return
        }
        _workState.value = WorkState.Loading()
        viewModelScope.launch(context = coroutineContext()) {
            _workState.value = WorkState.Loading()

            val accounts: List<AccountInformation>? = account?.let { (accountId, accountTypeId) ->
                listOfNotNull(repository.accountInformation(accountId, accountTypeId).let {
                    when {
                        it == null -> {
                            report(Exception("Error while retrieving Information for account"))
                            null
                        }
                        it.lastSynced == null -> {
                            report(Exception("Error while retrieving Information for account (lastSynced)"))
                            null
                        }
                        else -> it
                    }
                })
            } ?: credentials.bank?.let {
                repository.importedAccounts(it.id)
            }

            if (accounts.isNullOrEmpty()) {
                error("Keine Konten")
                _workState.value = WorkState.Abort
                return@launch
            }

            doHBCI(
                credentials,
                work = { _, _, handle ->
                    val jobs: Map<AccountInformation, HBCIJob> = accounts.associateWith { accountInformation ->
                        val konto = Konto(
                            "DE",
                            accountInformation.blz ?: credentials.blz,
                            accountInformation.number,
                            accountInformation.subnumber
                        ).also {
                            it.name = accountInformation.name
                            it.iban = accountInformation.iban
                            it.bic = accountInformation.bic ?: credentials.bank?.bic
                        }


                        handle.newJob("KUmsAll").apply {
                            setParam("my", konto)
                            log("Setting my param to $konto")
                            setStartParam(accountInformation.lastSynced!!)
                            addToQueue()
                        }
                    }

                    val status: HBCIExecStatus = handle.execute()

                    if (!status.isOK) {
                        error(status.toString())
                        _workState.value = WorkState.Abort
                        return@doHBCI
                    }

                    var importCount = 0
                    jobs.forEach { (accountInformation, umsatzJob) ->
                        val result = umsatzJob.jobResult as GVRKUms
                        if (!result.isOK) {
                            error(result.toString())
                            _workState.value = WorkState.Abort
                            return@doHBCI
                        }
                        for (umsLine in result.flatData) {
                            with(converter) {
                                val (transaction, attributes: Map<out Attribute, String>) =
                                    umsLine.toTransaction(currencyContext, accountInformation.accountId, 1) //TODO
                                if (!isDuplicate(transaction, attributes[FinTsAttribute.CHECKSUM]!!)) {
                                    val id = ContentUris.parseId(transaction.save(contentResolver)!!)
                                    repository.saveTransactionAttributes(id, attributes)

                                    importCount++
                                }
                            }
                        }
                        setAccountLastSynced(accountInformation.accountId)

                    }
                    _workState.value =
                        WorkState.Success(
                            if (importCount > 0)
                                getQuantityString(
                                    R.plurals.transactions_imported,
                                    importCount,
                                    importCount
                                )
                            else
                                getString(R.string.transactions_imported_none)
                        )
                    logEvent(Tracker.EVENT_FINTS_TRANSACTIONS_LOADED, credentials)
                    if (credentials.bank?.asWellKnown == null) {
                        CrashHandler.report(Exception("Unknown bank: ${credentials.blz}"))
                    }
                },
                onError = {
                    error(it, credentials)
                    _workState.value = WorkState.Abort
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
        )?.useAndMapToList {
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
        if (_workState.value is WorkState.Loading) {
            log("Double click")
            return
        }
        _workState.value = WorkState.Loading()
        clearError()
        var successCount = 0
        viewModelScope.launch(context = coroutineContext()) {
            accounts.forEach { (konto, targetAccount) ->
                doHBCI(
                    bankingCredentials,
                    work = { _, _, handle ->

                        _workState.value = WorkState.Loading(
                            getString(
                                RF.string.progress_importing_account,
                                konto.iban
                            )
                        )
                        val umsatzJob: HBCIJob = handle.newJob("KUmsAll")
                        val kontoParam = konto.also {
                            if (it.bic == null) {
                                it.bic = bankingCredentials.bank?.bic
                            }
                        }
                        log("Setting my param to $kontoParam")
                        umsatzJob.setParam("my", kontoParam)
                        startDate?.let { umsatzJob.setStartParam(startDate) }

                        try {
                            umsatzJob.addToQueue()
                        } catch (e: Exception) {
                            error(e, bankingCredentials)
                            return@doHBCI
                        }

                        val status: HBCIExecStatus = handle.execute()

                        if (!status.isOK) {
                            error(status.toString())
                            return@doHBCI
                        }

                        val result = umsatzJob.jobResult as GVRKUms

                        if (!result.isOK) {
                            _workState.value = WorkState.Abort
                            error(result.toString())
                            return@doHBCI
                        }

                        val (accountId, accountType) = targetAccount.takeIf { it != 0L }?.also {
                            repository.updateAccount(it) {
                                put(KEY_BANK_ID, bank.id)
                            }
                        }?.let { id -> id to this@BankingViewModel.accounts.value.first { it.id == id }.type!! }
                            ?: run {
                            val accountType = repository.findAccountType(AccountType.BANK.name)!!
                            repository.createAccount(
                                konto.toAccount(
                                    bank,
                                    result.dataPerDay.firstOrNull()?.start?.value?.longValue ?: 0L
                                ).copy(type = accountType)
                            ).id to accountType
                        }

                        repository.saveAccountAttributes(accountId, konto.asAttributes)

                        for (umsLine in result.flatData) {
                            with(converter) {
                                val (transaction, transactionAttributes: Map<out Attribute, String>) = umsLine.toTransaction(
                                    currencyContext, accountId, accountType.id
                                )
                                val id = ContentUris.parseId(transaction.save(contentResolver)!!)
                                repository.saveTransactionAttributes(id, transactionAttributes)
                            }
                        }
                        setAccountLastSynced(accountId)
                        logEvent(Tracker.EVENT_FINTS_ACCOUNT_IMPORTED, bankingCredentials)
                        successCount++
                    },
                    onError = {
                        error(it, bankingCredentials)
                        _workState.value = WorkState.Abort
                    }
                )
            }
            licenceHandler.recordUsage(ContribFeature.BANKING)
            _workState.value = WorkState.Success(
                getQuantityString(R.plurals.accounts_imported, successCount, successCount)
            )
        }
    }

    fun deleteBank(bank: Bank) {
        passportFile(bank.blz, bank.userId).delete()
        passphraseFile(bank.blz, bank.userId).delete()
        repository.deleteBank(bank.id)
    }

    fun reset() {
        _workState.value = WorkState.Initial
        clearError()
    }

    private fun clearError() {
        _errorState.value = null
    }

    private fun passphraseFile(blz: String, user: String) =
        File(getApplication<MyApplication>().filesDir, "passphrase_${blz}_${user}.bin")

    private fun getPassPhraseRepository(blz: String, user: String) =
        PassphraseRepository(getApplication(), passphraseFile(blz, user)) {
            RandomStringGenerator
                .builder()
                .usingRandom { SecureRandom().nextInt(it) }
                .withinRange('a'.code, 'z'.code)
                .get()
                .generate(20)
                .toByteArray()
        }


    inner class MyHBCICallback(private val bankingCredentials: BankingCredentials) :
        AbstractHBCICallback() {

        private var selectedTanMedium: String? = null

        private var selectedSecMech: String? = null

        init {
            bankingCredentials.bank?.let {
                selectedTanMedium = selectedTanMedium(it.id)
                selectedSecMech = selectedSecMech(it.id)
            }
        }

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
            log("callback:$reason")
            when (reason) {
                NEED_PASSPHRASE_LOAD, NEED_PASSPHRASE_SAVE ->
                    retData.replace(
                        0, retData.length,
                        if (bankingCredentials.bank?.version == 1) {
                            log("Using legacy password (=PIN)")
                            bankingCredentials.password!!
                        } else {
                            log("Using new password (via encrypted file)")
                            bankingCredentials.blz + bankingCredentials.user
                            getPassPhraseRepository(
                                bankingCredentials.blz,
                                bankingCredentials.user
                            ).getPassphrase().toString(Charsets.UTF_8)
                        }
                    )

                NEED_PT_PIN -> retData.replace(0, retData.length, bankingCredentials.password!!)
                NEED_BLZ -> retData.replace(0, retData.length, bankingCredentials.blz)
                NEED_USERID -> retData.replace(0, retData.length, bankingCredentials.user)
                NEED_CUSTOMERID -> retData.replace(0, retData.length, bankingCredentials.user)
                NEED_PT_PHOTOTAN -> try {
                    val code = MatrixCode(retData.toString())

                    val bitmap = BitmapFactory.decodeByteArray(code.image, 0, code.image.size)
                    val tan = runBlocking {
                        requestTan(msg, bitmap).await()
                            ?: throw HBCI_Exception("TAN entry cancelled")
                    }
                    retData.replace(0, retData.length, tan)
                } catch (e: Exception) {
                    report(e)
                    throw HBCI_Exception(e)
                }

                NEED_PT_QRTAN -> try {
                    val code = QRCode(retData.toString(), msg)

                    val bitmap = BitmapFactory.decodeByteArray(code.image, 0, code.image.size)

                    val tan = runBlocking {
                        requestTan(code.message, bitmap).await()
                            ?: throw HBCI_Exception("TAN entry cancelled")
                    }
                    retData.replace(0, retData.length, tan)
                } catch (e: Exception) {
                    report(e)
                    throw HBCI_Exception(e)
                }

                NEED_PT_SECMECH -> {
                    val options = SecMech.parse(retData.toString())
                    retData.replace(0, retData.length,
                        if (options.size == 1) {
                            options[0].id
                        } else selectedSecMech.takeIf { pref -> options.any { it.id == pref } }
                            ?: runBlocking {
                                requestSecMech(options).await()?.let { (secMec, shouldPersist) ->
                                    selectedSecMech = secMec
                                    if (shouldPersist && bankingCredentials.bank != null) {
                                        persistSelectedSecMech(bankingCredentials.bank.id, secMec)
                                    }
                                    secMec
                                } ?: throw HBCI_Exception("Security mechanism selection cancelled")
                            }
                    )
                }

                NEED_PT_TAN -> {
                    val flicker = retData.toString()
                    if (flicker.isNotEmpty()) {
                        throwAndReport(HBCI_Exception("Flicker not yet implemented for ${bankingCredentials.blz} Please contact support@myexpenses.mobi !"))
                    } else {
                        val tan = runBlocking {
                            requestTan(msg, null).await()
                                ?: throw HBCI_Exception("TAN entry cancelled")
                        }
                        retData.replace(0, retData.length, tan)
                    }
                }

                NEED_PT_TANMEDIA -> {
                    val options = retData.toString().split("|")
                    retData.replace(0, retData.length,
                        if (options.size == 1) {
                            options[0]
                        } else selectedTanMedium.takeIf { options.contains(it) } ?: runBlocking {
                            requestTanMedium(options).await()?.let { (medium, shouldPersist) ->
                                selectedTanMedium = medium
                                if (shouldPersist && bankingCredentials.bank != null) {
                                    persistSelectedTanMedium(bankingCredentials.bank.id, medium)
                                }
                                medium
                            } ?: throw HBCI_Exception("TAN media selection cancelled")
                        }
                    )
                }

                NEED_PT_DECOUPLED -> {
                    runBlocking { requestPushTan(msg).await() }
                }

                HAVE_ERROR -> report(Throwable(msg))

                HAVE_INST_MSG -> _instMessage.update { msg }

                else -> {}
            }
        }

        override fun status(passport: HBCIPassport, statusTag: Int, o: Array<Any?>?) {
            log("status:$statusTag")
        }
    }

    private fun throwAndReport(throwable: Throwable) {
        report(throwable)
        throw throwable
    }

    private fun report(throwable: Throwable) {
        CrashHandler.report(throwable, BankingFeature.TAG)
    }

    suspend fun migrateBank(bank: Bank, passphrase: String): Result<Unit> =
        withContext(coroutineDispatcher) {
            suspendCoroutine { cont ->
                doHBCI(
                    bankingCredentials = BankingCredentials.fromBank(bank)
                        .copy(password = passphrase),
                    work = { _, _, _ ->
                        val passphraseRepository = getPassPhraseRepository(bank.blz, bank.userId)
                        passphraseRepository.storePassphrase(passphrase.toByteArray(Charsets.UTF_8))
                        contentResolver.update(
                            ContentUris.withAppendedId(TransactionProvider.BANKS_URI, bank.id),
                            ContentValues().also { it.put(KEY_VERSION, 2) },
                            null, null
                        )
                        cont.resume(ResultUnit)
                    },
                    onError = {
                        cont.resume(Result.failure(it))
                    }
                )
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
            query = "${DatabaseConstants.KEY_ACCOUNT_TYPE_LABEL} != '${AccountType.CASH.name}' AND $KEY_BANK_ID IS NULL",
            withAggregates = false
        ).stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )
    }

}