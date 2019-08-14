package org.totschnig.myexpenses.provider

import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.retrofit.ExchangeRatesApi
import org.totschnig.myexpenses.room.ExchangeRate
import org.totschnig.myexpenses.room.ExchangeRateDao
import timber.log.Timber
import java.io.IOException

class ExchangeRateRepository(val dao: ExchangeRateDao, val api: ExchangeRatesApi) {
    @Throws(IOException::class)
    suspend fun loadExchangeRate(other: String, base: String, date: LocalDate): Float {
        val rate = dao.getRate(base, other, date)
        val error : String?
        if (rate != null) {
            return rate
        } else {
            val response = api.getRate(date, other, base).execute()
            if (BuildConfig.DEBUG) {
                if (response.raw().cacheResponse() != null) {
                    Timber.i("Response was cached")
                }
                if (response.raw().networkResponse() != null) {
                    Timber.i("Response was from network")
                }
            }
            if (response.isSuccessful) {
                response.body()?.let { result ->
                    result.rates.get(other)?.let {
                        dao.insert(ExchangeRate(base, other, result.date, it))
                        return it
                    }
                }
                error = "Unable to retrieve rate"
            } else {
                error = response.errorBody()?.let {
                    JSONObject(it.string()).getString("error")
                } ?: "Unknown Error"
            }
        }
        throw IOException(error)
    }
}