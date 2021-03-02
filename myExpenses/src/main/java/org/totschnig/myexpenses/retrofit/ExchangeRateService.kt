package org.totschnig.myexpenses.retrofit

import org.jetbrains.annotations.NotNull
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

enum class ExchangeRateSource {
    RATESAPI, OPENEXCHANGERATES;
}

class MissingAppIdException : java.lang.IllegalStateException()

class ExchangeRateService(private val ratesApi: @NotNull RatesApi, val openExchangeRatesApi: @NotNull OpenExchangeRatesApi) {
    private val ECP_SUPPORTED_CURRENCIES = arrayOf(
            "USD", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK", "CHF", "ISK", "NOK",
            "HRK", "RUB", "TRY", "AUD", "BRL", "CAD", "CNY", "HKD", "IDR", "ILS", "INR", "KRW", "MXN",
            "MYR", "NZD", "PHP", "SGD", "THB", "ZAR")
    private var appId = ""
    var source = ExchangeRateSource.RATESAPI
    fun getRate(date: LocalDate, symbol: String, base: String): Pair<LocalDate, Float> = when (source) {
        ExchangeRateSource.RATESAPI -> {
            val error: String
            val response = ratesApi.getRate(date, symbol, base).execute()
            log(response)
            if (response.isSuccessful) {
                response.body()?.let { result ->
                    result.rates[symbol]?.let {
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
            if (appId == "") throw MissingAppIdException()
            val error: String
            val response = openExchangeRatesApi.getRate(date,
                    "$symbol,$base", appId).execute()
            log(response)
            error = if (response.isSuccessful) {
                response.body()?.let { result ->
                    val otherRate = result.rates[symbol]
                    val baseRate = result.rates[base]
                    if (otherRate != null && baseRate != null) {
                        return Pair(toLocalDate(result.timestamp), otherRate / baseRate)
                    }
                }
                "Unable to retrieve rate"
            } else {
                response.errorBody()?.let {
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

    private fun toLocalDate(timestamp: Long): LocalDate {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()).toLocalDate()
    }

    fun configure(prefHandler: @NotNull PrefHandler): ExchangeRateSource {
        source = try {
            ExchangeRateSource.valueOf(prefHandler.requireString(PrefKey.EXCHANGE_RATE_PROVIDER, ExchangeRateSource.RATESAPI.name))
        } catch (e: IllegalArgumentException) {
            ExchangeRateSource.RATESAPI
        }
        appId = prefHandler.requireString(PrefKey.OPEN_EXCHANGE_RATES_APP_ID, "")
        return source
    }
}