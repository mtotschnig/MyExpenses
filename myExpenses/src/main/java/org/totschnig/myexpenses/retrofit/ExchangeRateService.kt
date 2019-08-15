package org.totschnig.myexpenses.retrofit

import org.jetbrains.annotations.NotNull
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.BuildConfig
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

enum class ExchangeRateSource {
    RATESAPI, OPENEXCHANGERATES;
}

class ExchangeRateService(val ratesApi: @NotNull RatesApi, val openExchangeRatesApi: @NotNull OpenExchangeRatesApi) {
    val ECP_SUPPORTED_CURRENCIES = arrayOf(
            "USD", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK", "CHF", "ISK", "NOK",
            "HRK", "RUB", "TRY", "AUD", "BRL", "CAD", "CNY", "HKD", "IDR", "ILS", "INR", "KRW", "MXN",
            "MYR", "NZD", "PHP", "SGD", "THB", "ZAR")
    val APP_ID = "TODO"
    fun getRate(date: LocalDate, symbol: String, base: String, source: ExchangeRateSource): Pair<LocalDate, Float> = when (source) {
        ExchangeRateSource.RATESAPI -> {
            val error: String
            val response = ratesApi.getRate(date, symbol, base).execute()
            log(response)
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
        ExchangeRateSource.OPENEXCHANGERATES -> {
            val error: String
            val response = openExchangeRatesApi.getRate(date,
                    symbol+","+base, APP_ID).execute()
            log(response)
            if (response.isSuccessful) {
                response.body()?.let { result ->
                    val otherRate = result.rates.get(symbol)
                    val baseRate = result.rates.get(base)
                    if (otherRate != null && baseRate != null) {
                        return Pair(toLocalDate(result.timestamp), otherRate / baseRate)
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

    fun log(response: Response<*>) {
        if (BuildConfig.DEBUG) {
            if (response.raw().cacheResponse() != null) {
                Timber.i("Response was cached")
            }
            if (response.raw().networkResponse() != null) {
                Timber.i("Response was from network")
            }
        }
    }

    fun toLocalDate(timestamp: Long): LocalDate {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()).toLocalDate()
    }
}