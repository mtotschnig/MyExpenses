package org.totschnig.myexpenses.retrofit

import org.jetbrains.annotations.NotNull
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

enum class ExchangeRateSource {
    EXCHANGE_RATE_HOST, OPENEXCHANGERATES;
}

data class Configuration(val source: ExchangeRateSource, val openExchangeRatesAppId: String = "")

class MissingAppIdException : java.lang.IllegalStateException()

class ExchangeRateService(
    val exchangeRateHost: @NotNull ExchangeRateHost,
    val openExchangeRates: @NotNull OpenExchangeRates
) {
    fun getRate(
        configuration: Configuration,
        date: LocalDate,
        symbol: String,
        base: String
    ): Pair<LocalDate, Float> = when (configuration.source) {
        ExchangeRateSource.EXCHANGE_RATE_HOST -> {
            val error: String
            val response = exchangeRateHost.getRate(date, date, symbol, base).execute()
            log(response)
            error = if (response.isSuccessful) {
                response.body()?.let { result ->
                    result.rates[date]?.get(symbol)?.let {
                        return Pair(date, it)
                    }
                }
                "Unable to retrieve rate"
            } else {
                response.errorBody()?.string() ?: "Unknown Error"
            }
            throw IOException(error)
        }
        ExchangeRateSource.OPENEXCHANGERATES -> {
            if (configuration.openExchangeRatesAppId == "") throw MissingAppIdException()
            val error: String
            val response = openExchangeRates.getRate(
                date,
                "$symbol,$base", configuration.openExchangeRatesAppId
            ).execute()
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
            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()
        ).toLocalDate()
    }

    fun configuration(prefHandler: @NotNull PrefHandler): Configuration {
        val default = ExchangeRateSource.EXCHANGE_RATE_HOST
        return Configuration(
            try {
                ExchangeRateSource.valueOf(
                    prefHandler.requireString(
                        PrefKey.EXCHANGE_RATE_PROVIDER,
                        default.name
                    )
                )
            } catch (e: IllegalArgumentException) {
                default
            }, prefHandler.requireString(PrefKey.OPEN_EXCHANGE_RATES_APP_ID, "")
        )
    }
}