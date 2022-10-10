package org.totschnig.myexpenses.retrofit

import org.jetbrains.annotations.NotNull
import org.json.JSONObject
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ExchangeRateSource {
    EXCHANGE_RATE_HOST,

    @Suppress("SpellCheckingInspection")
    OPENEXCHANGERATES;
    companion object {
        val defaultSource = EXCHANGE_RATE_HOST
    }
}

data class Configuration(val source: ExchangeRateSource, val openExchangeRatesAppId: String = "")

class MissingAppIdException : java.lang.IllegalStateException()

class ExchangeRateService(
    private val exchangeRateHost: @NotNull ExchangeRateHost,
    val openExchangeRates: @NotNull OpenExchangeRates
) {
    fun getRate(
        configuration: Configuration,
        date: LocalDate,
        symbol: String,
        base: String
    ): Pair<LocalDate, Float> = when (configuration.source) {
        ExchangeRateSource.EXCHANGE_RATE_HOST -> {
            val today = LocalDate.now()
            val error = if (date < today) {
                val response = exchangeRateHost.getTimeSeries(date, date, symbol, base).execute()
                log(response)
                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        result.rates[date]?.get(symbol)?.let {
                            return Pair(date, it)
                        }
                    }
                    null
                } else {
                    response.errorBody()?.string() ?: "Unknown Error"
                }
            } else {
                val response = exchangeRateHost.getLatest(symbol, base).execute()
                log(response)
                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        result.rates[symbol]?.let {
                            return Pair(today, it)
                        }
                    }
                    null
                } else {
                    response.errorBody()?.string() ?: "Unknown Error"
                }
            }
            throw IOException(error ?: "Unable to retrieve data")
        }
        ExchangeRateSource.OPENEXCHANGERATES -> {
            if (configuration.openExchangeRatesAppId == "") throw MissingAppIdException()
            val today = LocalDate.now()
            val call = if (date < today) {
                openExchangeRates.getHistorical(
                    date,
                    "$symbol,$base", configuration.openExchangeRatesAppId
                )
            } else {
                openExchangeRates.getLatest(
                    "$symbol,$base", configuration.openExchangeRatesAppId
                )
            }
            val response = call.execute()
            log(response)
            val error = if (response.isSuccessful) {
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
                    JSONObject(it.string()).getString("description")
                } ?: "Unknown Error"
            }
            throw IOException(error)
    }
}

fun log(response: Response<*>) {
    if (BuildConfig.DEBUG) {
        if (response.raw().cacheResponse != null) {
            Timber.i("Response was cached")
        }
        if (response.raw().networkResponse != null) {
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
    @Suppress("SpellCheckingInspection")
    val preferenceValue = prefHandler.requireString(
        PrefKey.EXCHANGE_RATE_PROVIDER,
        ExchangeRateSource.defaultSource.name
    ).takeIf { it != "RATESAPI" }
    val source = enumValueOrDefault(preferenceValue, ExchangeRateSource.defaultSource)
    return Configuration(
        source, prefHandler.requireString(PrefKey.OPEN_EXCHANGE_RATES_APP_ID, "")
    )
}
}