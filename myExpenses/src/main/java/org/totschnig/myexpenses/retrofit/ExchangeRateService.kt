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
    val ECP_SUPPORTED_CURRENCIES = arrayOf(
            "USD", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK", "CHF", "ISK", "NOK",
            "HRK", "RUB", "TRY", "AUD", "BRL", "CAD", "CNY", "HKD", "IDR", "ILS", "INR", "KRW", "MXN",
            "MYR", "NZD", "PHP", "SGD", "THB", "ZAR")
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
                if (symbol in ECP_SUPPORTED_CURRENCIES && base in ECP_SUPPORTED_CURRENCIES) {
                    error = response.errorBody()?.let {
                        JSONObject(it.string()).getString("error")
                    } ?: "Unknown Error"
                } else {
                    throw UnsupportedOperationException()
                }
            }
            throw IOException(error)
        }
    }
}