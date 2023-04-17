package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.ExchangeRateRepository
import org.totschnig.myexpenses.retrofit.MissingApiKeyException
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

class ExchangeRateViewModel(val application: MyApplication) {
    private val exchangeRate: MutableLiveData<Double> = MutableLiveData()
    private val error: MutableLiveData<String> = MutableLiveData()

    @Inject
    lateinit var repository: ExchangeRateRepository
    private val viewModelJob = SupervisorJob()
    private val bgScope = CoroutineScope(Dispatchers.Default + viewModelJob)

    init {
        bgScope.launch {
            application.appComponent.inject(this@ExchangeRateViewModel)
        }
    }

    fun getData(): LiveData<Double> = exchangeRate
    fun getError(): LiveData<String> = error

    fun loadExchangeRate(other: String, base: String, date: LocalDate) {
        bgScope.launch {
            try {
                postResult(repository.loadExchangeRate(other, base, date))
            } catch (e: Exception) {
                postException(other, base, e)
            }
        }
    }

    private suspend fun postResult(rate: Double) = withContext(Dispatchers.Main) {
        exchangeRate.postValue(rate)
    }

    private suspend fun postException(other: String, base: String, exception: java.lang.Exception) =
        withContext(Dispatchers.Main) {
            if (exception !is IOException &&
                exception !is UnsupportedOperationException &&
                exception !is MissingApiKeyException
            ) {
                CrashHandler.report(exception)
            }
            error.postValue(
                when (exception) {
                    is java.lang.UnsupportedOperationException -> application.wrappedContext.getString(
                        R.string.exchange_rate_not_supported, other, base
                    )

                    is MissingApiKeyException -> application.wrappedContext.getString(R.string.pref_exchange_rates_api_key_summary, exception.source.host)
                    else -> exception.safeMessage
                }
            )
        }

    fun clear() {
        viewModelJob.cancel()
    }
}