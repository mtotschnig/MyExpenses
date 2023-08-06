package org.totschnig.myexpenses.viewmodel

import android.app.Application
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
import org.kapott.hbci.callback.AbstractHBCICallback
import org.kapott.hbci.exceptions.HBCI_Exception
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.HBCIVersion
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.addBank
import org.totschnig.myexpenses.db2.loadBanks
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.data.Bank
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.Properties

class BankingViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val tanFuture: CompletableDeferred<String?> = CompletableDeferred()

    private val _tanRequested = MutableLiveData(false)

    val tanRequested: LiveData<Boolean> = _tanRequested

    private val _addBankState: MutableStateFlow<AddBankState> = MutableStateFlow(AddBankState.Initial)
    val addBankState: StateFlow<AddBankState> = _addBankState

    sealed class AddBankState {
        object Initial: AddBankState()
        object Loading: AddBankState()

        data class Error(val messsage: String): AddBankState()

        object Done: AddBankState()
    }

    fun submitTan(tan: String?) {
        tanFuture.complete(tan)
        _tanRequested.postValue(false)
    }

    private fun log(msg: String) {
        Timber.tag("FinTS").i(msg)
    }

    private fun error(msg: String) {
        _addBankState.value = AddBankState.Error(msg)
    }

    fun addBankMock(bank: Bank) {
        viewModelScope.launch(context = coroutineContext()) {
/*            MockFinTS.init(MyHBCICallback(bank))
            val message = MockFinTS.getTan(null)
            Timber.tag("FinTS").i(message)*/

            val info = HBCIUtils.getBankInfo(bank.bankLeitZahl)
            if (info == null) {
                _addBankState.value = AddBankState.Error("${bank.bankLeitZahl} not found in the list of banks that support FinTS")
            } else {
                repository.addBank(org.totschnig.myexpenses.model2.Bank(info.blz, info.bic, info.name, bank.user))
                _addBankState.value = AddBankState.Done
            }
        }
    }

    fun addBank(bank: Bank) {
        _addBankState.value = AddBankState.Loading
        viewModelScope.launch(context = coroutineContext()) {
            System.setProperty(
                "javax.xml.parsers.DocumentBuilderFactory",
                "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"
            )
            val props = Properties().also {
                it["client.product.name"] = "02F84CA8EC793B72255C747B4"
                if(BuildConfig.DEBUG) {
                    it["log.loglevel.default"] = HBCIUtils.LOG_INTERN.toString()
                }
            }

            HBCIUtils.init(props, MyHBCICallback(bank))

            val info = HBCIUtils.getBankInfo(bank.bankLeitZahl)

            if (info == null) {
                error("${bank.bankLeitZahl} not found in the list of banks that support FinTS")
                return@launch
            }

            val passportFile = File(getApplication<MyApplication>().filesDir, "testpassport.dat")

            HBCIUtils.setParam("client.passport.default", "PinTan")

            HBCIUtils.setParam("client.passport.PinTan.init", "1")

            val passport = AbstractHBCIPassport.getInstance(passportFile).apply {
                country = "DE"
                host = info.pinTanAddress
                port = 443
                filterType = "Base64"
            }

            try {
                val handle = try {
                    HBCIHandler(HBCIVersion.HBCI_300.id, passport)
                } catch (e: Exception) {
                    error(Utils.getCause(e).safeMessage)
                    return@launch
                }

                try {
                    val konten = passport.accounts
                    if (konten == null || konten.isEmpty()) {
                        error("Keine Konten ermittelbar")
                        return@launch
                    }
                    log("Anzahl Konten: " + konten.size)
                    _addBankState.value = AddBankState.Done
                } finally {
                    handle.close()
                }
            } finally {
                passport?.close()
            }
        }
    }

    fun resetAddBankState() {
        _addBankState.value = AddBankState.Initial
    }

    inner class MyHBCICallback(private val bank: Bank) : AbstractHBCICallback() {
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
            Timber.tag("FinTS").i("callback:%d",reason)
            when (reason) {
                NEED_PASSPHRASE_LOAD, NEED_PASSPHRASE_SAVE -> {
                    retData.replace(0, retData.length, bank.password!!)
                }

                NEED_PT_PIN -> retData.replace(0, retData.length, bank.password!!)
                NEED_BLZ -> retData.replace(0, retData.length, bank.bankLeitZahl)
                NEED_USERID -> retData.replace(0, retData.length, bank.user)
                NEED_CUSTOMERID -> retData.replace(0, retData.length, bank.user)
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
                        Timber.e("SecMech Selection Dialog not yet implemented ().", retData.toString())
                    }
                    val firstOption = options[0]
                    retData.replace(0, retData.length, firstOption.substring(0, firstOption.indexOf(":")))
                }

                NEED_PT_TAN -> {
                    val flicker = retData.toString()
                    if (flicker.isNotEmpty()) {
                        TODO()
                    } else {
                        _tanRequested.postValue(true)
                        retData.replace(0, retData.length, runBlocking {
                            val result = tanFuture.await() ?: throw HBCI_Exception("TAN entry cancelled")
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
            Timber.tag("FinTS").i("status:%d",statusTag)
            o?.forEach {
                Timber.tag("FinTS").i(it.toString())
            }
        }
    }

    val banks: StateFlow<List<org.totschnig.myexpenses.model2.Bank>>
        get() = repository.loadBanks().stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                emptyList()
            )
}