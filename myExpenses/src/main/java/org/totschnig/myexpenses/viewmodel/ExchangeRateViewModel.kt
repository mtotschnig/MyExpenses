package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.ExchangeRateRepository

class ExchangeRateViewModel(application: MyApplication) {
    private val exchangeRate: MutableLiveData<Float> = MutableLiveData()
    private val error: MutableLiveData<Exception> = MutableLiveData()
    private val repository: ExchangeRateRepository = application.appComponent.exchangeRateRepository()
    private val viewModelJob = SupervisorJob()
    private val bgScope = CoroutineScope(Dispatchers.Default + viewModelJob)

    fun getData(): LiveData<Float> = exchangeRate
    fun getError(): LiveData<Exception> = error

    fun loadExchangeRate(other: String, base: String, date: LocalDate) {
        bgScope.launch {
            try {
                val rate = repository.loadExchangeRate(other, base, date)
                withContext(Dispatchers.Main) {
                    exchangeRate.postValue(rate)
                }
            } catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    error.postValue(e)
                }
            }
        }
    }

    fun clear() {
        viewModelJob.cancel()
    }
}