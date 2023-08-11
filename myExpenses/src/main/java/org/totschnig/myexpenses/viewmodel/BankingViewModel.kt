package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.forEach
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
import org.kapott.hbci.structures.Value
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.createBank
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.deleteBank
import org.totschnig.myexpenses.db2.loadBanks
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.BankingCredentials
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Properties

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

    sealed class WorkState {
        object Initial : WorkState()
        data class Loading(val message: String) : WorkState()

        data class Error(val message: String) : WorkState()

        data class AccountsLoaded(val bank: Bank, val accounts: List<Konto>) : WorkState()

        object Done : WorkState()
    }

    fun submitTan(tan: String?) {
        tanFuture.complete(tan)
        _tanRequested.postValue(false)
    }

    private fun log(msg: String) {
        Timber.tag("FinTS").i(msg)
    }

    private fun error(msg: String) {
        _workState.value = WorkState.Error(msg)
    }

    private fun initHBCI(bankingCredentials: BankingCredentials): BankInfo? {
        HBCIUtils.init(hbciProperties, MyHBCICallback(bankingCredentials))
        HBCIUtils.setParam("client.passport.default", "PinTan")
        HBCIUtils.setParam("client.passport.PinTan.init", "1")

        val info = HBCIUtils.getBankInfo(bankingCredentials.blz)

        if (info == null) {
            HBCIUtils.doneThread()
            error("${bankingCredentials.blz} not found in the list of banks that support FinTS")
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
        work: (BankInfo, HBCIPassport, HBCIHandler) -> Unit
    ) {

        val info = initHBCI(bankingCredentials) ?: return

        val passportFile = buildPassportFile(info, bankingCredentials.user)

        val passport = buildPassport(info, passportFile)

        val handle = try {
            HBCIHandler(HBCIVersion.HBCI_300.id, passport)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e)
            }
            passport.close()
            passportFile.delete()
            HBCIUtils.doneThread()
            error(Utils.getCause(e).safeMessage)
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

    fun addBank(bankingCredentials: BankingCredentials) {
        _workState.value = WorkState.Loading("Loading information")

        if (banks.value.any { it.blz == bankingCredentials.blz && it.userId == bankingCredentials.user }) {
            error("Bank has already been added")
            return
        }
        viewModelScope.launch(context = coroutineContext()) {
            doHBCI(bankingCredentials) { info, passport, _ ->
                val bank = repository.createBank(
                    Bank(
                        blz = info.blz,
                        bic = info.bic,
                        bankName = info.name,
                        userId = bankingCredentials.user
                    )
                )
                val accounts = passport.accounts
                if (accounts == null || accounts.isEmpty()) {
                    error("Keine Konten ermittelbar")
                } else {
                    _workState.value = WorkState.AccountsLoaded(bank, accounts.asList())
                }
            }
        }
    }

    fun Konto.toAccount(bank: Bank, openingBalance: Long) = Account(
        label = bank.bankName,
        iban = iban,
        currency = curr,
        type = AccountType.BANK,
        bankId = bank.id,
        openingBalance = openingBalance
    )

    fun importAccounts(bankingCredentials: BankingCredentials, bank: Bank, accounts: List<Konto>) {
        val eur = currencyContext.get("EUR")
        viewModelScope.launch(context = coroutineContext()) {
            accounts.forEach {

                doHBCI(bankingCredentials) { _, _, handle ->

                    _workState.value = WorkState.Loading("Importing account ${it.iban}")

                    val umsatzJob: HBCIJob = handle.newJob("KUmsAll")
                    log("jobRestrictions : " + umsatzJob.jobRestrictions.toString())
                    umsatzJob.setParam("my", it)
                    umsatzJob.setParam(
                        "startdate",
                        Date.from(
                            LocalDate.now().minusDays(10).atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                        )
                    )

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
                        it.toAccount(bank, result.dataPerDay.first().start.value.longValue)
                    )
                    log("created account in db with id ${dbAccount.id}")
                    for (umsLine in result.flatData) {
                        log(umsLine.toString())
                        umsLine.toTransaction(dbAccount.id, eur, repository).save()
                    }
                }
            }
            _workState.value = WorkState.Done
        }
    }

    fun resetAddBankState() {
        _workState.value = WorkState.Initial
    }

    fun deleteBank(id: Long) {
        repository.deleteBank(id)
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