package org.totschnig.myexpenses.retrofit

import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.BuildConfig
import timber.log.Timber
import java.io.IOException

enum class ExchangeRateSource {
    RATESAPI;
}

class ExchangeRateService(val ratesApi: ExchangeRatesApi) {
    fun getRate(date: LocalDate, symbol: String, base: String, source: ExchangeRateSource): Pair<LocalDate, Float> = when (source) {
        ExchangeRateSource.RATESAPI -> {
            val error: String
            val response = ratesApi.getRate(date, symbol, base).execute()
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
                    result.rates.get(symbol)?.let {
                        return Pair(result.date, it)
                    }
                }
                error = "Unable to retrieve rate"
            } else {
                error = response.errorBody()?.let {
                    JSONObject(it.string()).getString("error")
                } ?: "Unknown Error"
            }
            throw IOException(error)
        }
    }
}