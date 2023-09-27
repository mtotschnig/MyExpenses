package org.totschnig.myexpenses.retrofit

import okhttp3.ResponseBody
import org.jetbrains.annotations.NotNull
import org.json.JSONObject
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import retrofit2.HttpException
import retrofit2.await
import java.io.IOException
import java.lang.UnsupportedOperationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

sealed class ExchangeRateSource(val id: String, val host: String) {

    fun convertError(e: HttpException) = e.response()?.errorBody()?.let { body ->
        extractError(body)?.let { IOException(it) }
    } ?: e

    abstract fun extractError(body: ResponseBody): String?

    companion object {

        val values = arrayOf(Frankfurter, OpenExchangeRates, CoinApi)

        fun preferredSource(prefHandler: PrefHandler) =
            preferredSource(prefHandler.getString(PrefKey.EXCHANGE_RATE_PROVIDER, null))

        fun preferredSource(preferenceValue: String?) =
            values.firstOrNull { it.id == preferenceValue } ?: Frankfurter
    }

    data object Frankfurter : ExchangeRateSource("FRANKFURTER", "api.frankfurter.app") {

        val SUPPORTED_CURRENCIES = arrayOf(
            "AUD",
            "BGN",
            "BRL",
            "CAD",
            "CHF",
            "CNY",
            "CZK",
            "DKK",
            "EUR",
            "GBP",
            "HKD",
            "HUF",
            "IDR",
            "ILS",
            "INR",
            "ISK",
            "JPY",
            "KRW",
            "MXN",
            "MYR",
            "NOK",
            "NZD",
            "PHP",
            "PLN",
            "RON",
            "SEK",
            "SGD",
            "THB",
            "TRY",
            "USD",
            "ZAR"
        )

        override fun extractError(body: ResponseBody): String =
            JSONObject(body.string()).getString("message")

    }

    sealed class SourceWithApiKey(
        val prefKey: PrefKey,
        host: String,
        id: String
    ) : ExchangeRateSource(id, host) {
        fun requireApiKey(prefHandler: PrefHandler): String =
            prefHandler.getString(prefKey)
                ?: throw MissingApiKeyException(this)
    }

    data object OpenExchangeRates : SourceWithApiKey(
        prefKey = PrefKey.OPEN_EXCHANGE_RATES_APP_ID,
        host = "openexchangerates.com",
        id = "OPENEXCHANGERATES"
    ) {
        override fun extractError(body: ResponseBody): String =
            JSONObject(body.string()).getString("description")
    }

    data object CoinApi : SourceWithApiKey(
        prefKey = PrefKey.COIN_API_API_KEY,
        host = "coinapi.io",
        id = "COIN_API"
    ) {
        override fun extractError(body: ResponseBody): String =
            JSONObject(body.string()).getString("error")
    }
}

class MissingApiKeyException(val source: ExchangeRateSource.SourceWithApiKey) :
    java.lang.IllegalStateException("${source.prefKey.name} not configured")

class ExchangeRateService(
    private val frankfurter: @NotNull Frankfurter,
    private val openExchangeRates: @NotNull OpenExchangeRates,
    private val coinApi: @NotNull CoinApi
) {
    suspend fun getRate(
        source: ExchangeRateSource,
        apiKey: String?,
        date: LocalDate,
        symbol: String,
        base: String
    ): Pair<LocalDate, Double> = try {
        when (source) {
            ExchangeRateSource.Frankfurter -> {
                if (symbol in ExchangeRateSource.Frankfurter.SUPPORTED_CURRENCIES && base in ExchangeRateSource.Frankfurter.SUPPORTED_CURRENCIES) {
                    val today = LocalDate.now()
                    val (dateOfResult, result) = if (date < today) {
                        date to frankfurter.getHistorical(date, symbol, base).await()
                    } else {
                        today to frankfurter.getLatest(symbol, base).await()
                    }
                    result.rates[symbol]?.let {
                        dateOfResult to it
                    } ?: throw IOException("Unable to retrieve data")
                } else {
                    throw UnsupportedOperationException()
                }
            }

            is ExchangeRateSource.OpenExchangeRates -> {
                requireNotNull(apiKey)
                val today = LocalDate.now()
                val call = if (date < today) {
                    openExchangeRates.getHistorical(date, "$symbol,$base", apiKey)
                } else {
                    openExchangeRates.getLatest("$symbol,$base", apiKey)
                }
                val result = call.await()
                val otherRate = result.rates[symbol]
                val baseRate = result.rates[base]
                if (otherRate != null && baseRate != null) {
                    toLocalDate(result.timestamp) to otherRate / baseRate
                } else throw IOException("Unable to retrieve data")
            }

            ExchangeRateSource.CoinApi -> {
                requireNotNull(apiKey)
                val today = LocalDate.now()
                if (date < today) {
                    val call = coinApi.getHistory(base, symbol, date, date.plusDays(1), apiKey)
                    val result = call.await().first()
                    date to arrayOf(
                        result.rate_close,
                        result.rate_high,
                        result.rate_low,
                        result.rate_close
                    ).average()
                } else {
                    val call = coinApi.getExchangeRate(base, symbol, apiKey)
                    val result = call.await()
                    LocalDate.now() to result.rate
                }
            }
        }
    } catch (e: HttpException) {
        throw source.convertError(e)
    }

    private fun toLocalDate(timestamp: Long): LocalDate {
        return ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()
        ).toLocalDate()
    }
}