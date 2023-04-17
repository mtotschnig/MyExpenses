package org.totschnig.myexpenses.retrofit

import org.jetbrains.annotations.NotNull
import org.json.JSONObject
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

sealed class ExchangeRateSource(val id: String) {

    companion object {

        fun preferredSource(prefHandler: PrefHandler) =
            preferredSource(prefHandler.getString(PrefKey.EXCHANGE_RATE_PROVIDER, null))

        fun preferredSource(preferenceValue: String?) = when (preferenceValue) {
            OpenExchangeRates.id -> OpenExchangeRates
            else -> ExchangeRateHost
        }
    }

    object ExchangeRateHost : ExchangeRateSource("EXCHANGE_RATE_HOST")

    sealed class SourceWithApiKey(
        val prefKey: PrefKey,
        val host: String,
        id: String
    ): ExchangeRateSource(id) {
        fun requireApiKey(prefHandler: PrefHandler): String =
            prefHandler.getString(prefKey)
                ?: throw MissingApiKeyException(this)
    }

    object OpenExchangeRates : SourceWithApiKey(
        prefKey = PrefKey.OPEN_EXCHANGE_RATES_APP_ID,
        host = "openexchangerates.com",
        id = "OPENEXCHANGERATES"
    )
}

class MissingApiKeyException(val source: ExchangeRateSource.SourceWithApiKey) :
    java.lang.IllegalStateException("${source.prefKey.name} not configured")

class ExchangeRateService(
    private val exchangeRateHost: @NotNull ExchangeRateHost,
    private val openExchangeRates: @NotNull OpenExchangeRates
) {
    fun getRate(
        source: ExchangeRateSource,
        apiKey: String?,
        date: LocalDate,
        symbol: String,
        base: String
    ): Pair<LocalDate, Float> = when (source) {
        ExchangeRateSource.ExchangeRateHost -> {
            val today = LocalDate.now()
            val errorResponse = if (date < today) {
                val response = exchangeRateHost.getTimeSeries(date, date, symbol, base).execute()
                log(response)
                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        result.rates[date]?.get(symbol)?.let {
                            return Pair(date, it)
                        }
                    }
                    null
                } else response
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
                } else response
            }
            throw IOException(
                if (errorResponse != null) {
                    (errorResponse.errorBody()?.string()?.takeIf { it.isNotEmpty() }
                        ?: "Unknown error") + " (${errorResponse.code()})"
                } else "Unable to retrieve data"

            )
        }

        is ExchangeRateSource.OpenExchangeRates -> {
            requireNotNull(apiKey)
            val today = LocalDate.now()
            val call = if (date < today) {
                openExchangeRates.getHistorical(
                    date,
                    "$symbol,$base", apiKey
                )
            } else {
                openExchangeRates.getLatest(
                    "$symbol,$base", apiKey
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
}