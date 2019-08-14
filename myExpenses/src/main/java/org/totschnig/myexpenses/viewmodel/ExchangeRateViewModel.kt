package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.ExchangeRateRepository
import org.totschnig.myexpenses.room.ExchangeRateDatabase
import java.io.IOException

class ExchangeRateViewModel(application: Application) : AndroidViewModel(application) {
    private val exchangeRate: MutableLiveData<Float> = MutableLiveData()
    private val error: MutableLiveData<String> = MutableLiveData()
    private val repository: ExchangeRateRepository
    private val viewModelJob = SupervisorJob()
    private val bgScope = CoroutineScope(Dispatchers.Default + viewModelJob)
    var date: LocalDate = LocalDate.now()

    init {
        repository = ExchangeRateRepository(
                ExchangeRateDatabase.getDatabase(application).exchangeRateDao(),
                (application as MyApplication).appComponent.exchangeRatesApi())
    }

    fun getData(): LiveData<Float> = exchangeRate
    fun getError(): LiveData<String> = error

    fun loadExchangeRate(other: String, base: String) {
        bgScope.launch {
            try {
                val rate = repository.loadExchangeRate(other, base, date)
                withContext(Dispatchers.Main) {
                    exchangeRate.postValue(rate)
                }
            } catch(e: IOException) {
                withContext(Dispatchers.Main) {
                    error.postValue(e.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}