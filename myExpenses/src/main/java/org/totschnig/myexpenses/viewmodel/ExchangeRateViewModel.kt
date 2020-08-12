package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.ExchangeRateRepository
import org.totschnig.myexpenses.retrofit.MissingAppIdException
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException
import javax.inject.Inject

class ExchangeRateViewModel(application: MyApplication) {
    private val exchangeRate: MutableLiveData<Float> = MutableLiveData()
    private val error: MutableLiveData<Exception> = MutableLiveData()
    @Inject
    lateinit var repository: ExchangeRateRepository
    private val viewModelJob = SupervisorJob()
    private val bgScope = CoroutineScope(Dispatchers.Default + viewModelJob)

    init {
        bgScope.launch {
            application.appComponent.inject(this@ExchangeRateViewModel)
        }
    }

    fun getData(): LiveData<Float> = exchangeRate
    fun getError(): LiveData<Exception> = error

    fun loadExchangeRate(other: String, base: String, date: LocalDate) {
        bgScope.launch {
            try {
                postResult(repository.loadExchangeRate(other, base, date))
            } catch (e: Exception) {
                postException(e)
            }
        }
    }

    private suspend fun postResult(rate: Float) = withContext(Dispatchers.Main) {
        exchangeRate.postValue(rate)
    }

    private suspend fun postException(exception: java.lang.Exception) = withContext(Dispatchers.Main) {
        when (exception) {
            is IOException, is UnsupportedOperationException, is MissingAppIdException -> {
            }
            else -> CrashHandler.report(exception)

        }
        error.postValue(exception)
    }

    fun clear() {
        viewModelJob.cancel()
    }
}