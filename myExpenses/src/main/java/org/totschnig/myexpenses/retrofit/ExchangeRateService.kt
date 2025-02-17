package org.totschnig.myexpenses.retrofit

import okhttp3.ResponseBody
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

sealed class ExchangeRateSource(val id: String, val host: String) {

    open val limitToOneRequestPerDay: Boolean = false

    fun convertError(e: HttpException) = e.response()?.errorBody()?.let { body ->
        extractError(body)?.let { IOException(it) }
    } ?: e

    abstract fun extractError(body: ResponseBody): String?

    open fun isSupported(vararg currency: String) = true

    companion object {

        fun getById(id: String) = values.first { it.id == id }

        val values = arrayOf(Frankfurter, OpenExchangeRates, CoinApi)

        fun configuredSources(prefHandler: PrefHandler) =
            configuredSources(prefHandler.getStringSet(PrefKey.EXCHANGE_RATE_PROVIDER))

        fun configuredSources(preferenceValue: Set<String>?) = preferenceValue?.let { configured ->
            values.filter { configured.contains(it.id) }
        }?.toSet() ?: emptySet()
    }

    data object Frankfurter : ExchangeRateSource("FRANKFURTER", "api.frankfurter.app") {

        override val limitToOneRequestPerDay = true

        override fun isSupported(vararg currency: String): Boolean {
            return SUPPORTED_CURRENCIES.containsAll(currency.toList())
        }

        val SUPPORTED_CURRENCIES = listOf(
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
        id: String,
    ) : ExchangeRateSource(id, host) {
        fun getApiKey(prefHandler: PrefHandler) = prefHandler.getString(prefKey)
        fun requireApiKey(prefHandler: PrefHandler): String =
            getApiKey(prefHandler) ?: throw MissingApiKeyException(this)
    }

    data object OpenExchangeRates : SourceWithApiKey(
        prefKey = PrefKey.OPEN_EXCHANGE_RATES_APP_ID,
        host = "openexchangerates.org",
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
    private val frankfurter: Frankfurter,
    private val openExchangeRates: OpenExchangeRates,
    private val coinApi: CoinApi,
) {

    suspend fun getLatest(
        source: ExchangeRateSource,
        apiKey: String?,
        base: String,
        symbols: List<String>,
    ): List<Pair<LocalDate, Double>> {
        return try {
            val symbolsArg = symbols.joinToString(",")
            when (source) {
                ExchangeRateSource.Frankfurter -> {
                    if (source.isSupported(base, *symbols.toTypedArray())) {
                        val result = frankfurter.getLatest(symbolsArg, base).await()
                        symbols.map { symbol ->
                            result.date to 1.0 / (result.rates[symbol]
                                ?: throw IOException("Unable to retrieve data for $symbol"))
                        }
                    } else {
                        throw UnsupportedOperationException()
                    }
                }

                ExchangeRateSource.CoinApi -> {
                    requireNotNull(apiKey)
                    val resultList = coinApi.getAllCurrent(base, symbolsArg, apiKey).await().rates
                    symbols.map { symbol ->
                        val result = resultList.find { it.asset_id_quote == symbol }
                            ?: throw IOException("Unable to retrieve data for $symbol")
                        parseIso8601(result.time) to result.rate
                    }
                }

                ExchangeRateSource.OpenExchangeRates -> {
                    requireNotNull(apiKey)
                    val result =
                        openExchangeRates.getLatest("$symbolsArg,$base", apiKey)
                            .await()
                    val baseRate =
                        result.rates[base] ?: throw IOException("Unable to retrieve data")
                    val localDate = toLocalDate(result.timestamp)
                    symbols.map { symbol ->
                        localDate to baseRate / (result.rates[symbol]
                            ?: throw IOException("Unable to retrieve data $symbol"))
                    }
                }
            }
        } catch (e: HttpException) {
            throw source.convertError(e)
        }
    }

    /**
     * Load the value of 1 unit of base currency expressed in symbol
     */
    suspend fun getRate(
        source: ExchangeRateSource,
        apiKey: String?,
        date: LocalDate,
        symbol: String,
        base: String,
    ): Pair<LocalDate, Double> = try {
        val today = LocalDate.now()
        when (source) {
            ExchangeRateSource.Frankfurter -> {
                if (source.isSupported(symbol, base)) {
                    val result = (if (date < today) {
                        frankfurter.getHistorical(date, symbol, base)
                    } else {
                        frankfurter.getLatest(symbol, base)
                    }).await()
                    result.date to (result.rates[symbol]
                        ?: throw IOException("Unable to retrieve data for $symbol"))
                } else {
                    throw UnsupportedOperationException()
                }
            }

            ExchangeRateSource.OpenExchangeRates -> {
                requireNotNull(apiKey)
                val result = (if (date < today) {
                    openExchangeRates.getHistorical(date, "$symbol,$base", apiKey)
                } else {
                    openExchangeRates.getLatest("$symbol,$base", apiKey)
                }).await()
                val otherRate = result.rates[symbol]
                val baseRate = result.rates[base]
                if (otherRate != null && baseRate != null) {
                    toLocalDate(result.timestamp) to otherRate / baseRate
                } else throw IOException("Unable to retrieve data for $symbol")
            }

            ExchangeRateSource.CoinApi -> {
                requireNotNull(apiKey)
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
                    parseIso8601(result.time) to result.rate
                }
            }
        }
    } catch (e: HttpException) {
        throw source.convertError(e)
    }

    private fun toLocalDate(timestamp: Long) =
        LocalDate.ofInstant(
            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()
        )

    private fun parseIso8601(input: String) =
        LocalDate.ofInstant(
            Instant.parse(input),
            ZoneId.systemDefault()
        )
}