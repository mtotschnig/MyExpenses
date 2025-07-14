package org.totschnig.myexpenses.retrofit

import okhttp3.ResponseBody
import org.json.JSONObject
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

sealed class ExchangeRateSource(val name: String) {
    object User : ExchangeRateSource("user")
    object Calculation : ExchangeRateSource("calculation")
    object Import: ExchangeRateSource("import")

    companion object {
        val values
            get() = arrayOf(
                ExchangeRateApi.Frankfurter,
                ExchangeRateApi.OpenExchangeRates,
                ExchangeRateApi.CoinApi,
                User,
                Calculation,
                Import
            )

        fun getByName(name: String) = values.first { it.name == name }
    }
}

sealed class ExchangeRateApi(val id: Int, name: String, val host: String) :
    ExchangeRateSource(name) {

    open val limitToOneRequestPerDay: Boolean = false

    open val hasTimeSeriesRequest: Boolean = true

    fun convertError(e: HttpException) = e.response()?.errorBody()?.let { body ->
        extractError(body)?.let { IOException(it) }
    } ?: e

    abstract fun extractError(body: ResponseBody): String?

    open fun isSupported(vararg currency: String) = true

    companion object {

        fun getById(id: Int) = values.find { it.id == id }
        fun getByName(name: String) = values.first { it.name == name }

        val values
            get() = arrayOf(Frankfurter, OpenExchangeRates, CoinApi)

        fun configuredSources(prefHandler: PrefHandler) =
            configuredSources(prefHandler.getStringSet(PrefKey.EXCHANGE_RATE_PROVIDER))

        fun configuredSources(preferenceValue: Set<String>?): Set<ExchangeRateApi> =
            if (preferenceValue?.isEmpty() == true) emptySet() else
                preferenceValue?.let { configured ->
                    values.filter { configured.contains(it.name) }
                }
                    ?.takeIf { it.isNotEmpty() }
                    ?.toSet()
                    ?: setOf(Frankfurter)
    }

    data object Frankfurter :
        ExchangeRateApi(R.id.FRANKFURTER_ID, "FRANKFURTER", "api.frankfurter.app") {

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
        id: Int,
        name: String,
        host: String,
        val prefKey: PrefKey,
    ) : ExchangeRateApi(id, name, host) {
        fun getApiKey(prefHandler: PrefHandler) = prefHandler.getString(prefKey)
            ?.takeIf { it.isNotEmpty() }
        fun requireApiKey(prefHandler: PrefHandler): String =
            getApiKey(prefHandler) ?: throw MissingApiKeyException(this)
    }

    data object OpenExchangeRates : SourceWithApiKey(
        prefKey = PrefKey.OPEN_EXCHANGE_RATES_APP_ID,
        host = "openexchangerates.org",
        id = R.id.OPENEXCHANGERATES_ID,
        name = "OPENEXCHANGERATES"
    ) {
        override val hasTimeSeriesRequest = false
        override fun extractError(body: ResponseBody): String =
            JSONObject(body.string()).getString("description")
    }

    data object CoinApi : SourceWithApiKey(
        prefKey = PrefKey.COIN_API_API_KEY,
        host = "coinapi.io",
        id = R.id.COIN_API_ID,
        name = "COIN_API"
    ) {
        override fun extractError(body: ResponseBody): String =
            JSONObject(body.string()).getString("error")
    }
}

class MissingApiKeyException(val source: ExchangeRateApi.SourceWithApiKey) :
    java.lang.IllegalStateException("${source.prefKey.name} not configured")

class ExchangeRateService(
    private val frankfurter: Frankfurter,
    private val openExchangeRates: OpenExchangeRates,
    private val coinApi: CoinApi,
) {

    /**
     * Load the values of 1 unit of each symbol expressed in base
     */
    suspend fun getLatest(
        source: ExchangeRateApi,
        apiKey: String?,
        base: String,
        symbols: List<String>,
    ): Pair<LocalDate, List<Double>> = try {
        if (!source.isSupported(base, *symbols.toTypedArray())) {
            throw UnsupportedOperationException()
        }
        val symbolsArg = symbols.joinToString(",")
        when (source) {
            ExchangeRateApi.Frankfurter -> {
                val result = frankfurter.getLatest(symbolsArg, base)
                result.date to symbols.map { symbol ->
                    1.0 / (result.rates[symbol]
                        ?: throw IOException("Unable to retrieve data for $symbol"))
                }
            }

            ExchangeRateApi.CoinApi -> {
                requireNotNull(apiKey)
                val resultList = coinApi.getAllCurrent(base, symbolsArg, apiKey).rates
                LocalDate.now() to symbols.map { symbol ->
                    val result = resultList.find { it.asset_id_quote == symbol }
                        ?: throw IOException("Unable to retrieve data for $symbol")
                    //We ignore the time returned as part of the response, since it might relate to a different
                    //day then the requested one (due to time zone difference)
                    result.rate
                }
            }

            ExchangeRateApi.OpenExchangeRates -> {
                requireNotNull(apiKey)
                val result =
                    openExchangeRates.getLatest("$symbolsArg,$base", apiKey)
                val baseRate =
                    result.rates[base] ?: throw IOException("Unable to retrieve data")
                LocalDate.now() to symbols.map { symbol ->
                    //We ignore the time returned as part of the response, since it might relate to a different
                    //day then the requested one (due to time zone difference)
                    baseRate / (result.rates[symbol]
                        ?: throw IOException("Unable to retrieve data $symbol"))
                }
            }
        }
    } catch (e: HttpException) {
        throw source.convertError(e)
    }

    /**
     * Load the value of 1 unit of base currency expressed in symbol
     */
    suspend fun getRate(
        source: ExchangeRateApi,
        apiKey: String?,
        date: LocalDate,
        base: String,
        symbol: String,
    ): Pair<LocalDate, Double> = try {
        if (!source.isSupported(symbol, base)) {
            throw UnsupportedOperationException()
        }
        val today = LocalDate.now()
        when (source) {
            ExchangeRateApi.Frankfurter -> {
                val result = (if (date < today) {
                    frankfurter.getHistorical(date, symbol, base)
                } else {
                    frankfurter.getLatest(symbol, base)
                })
                //Frankfurter only returns values for business days, e.g. when we request a rate for
                //Saturday or Sunday, it will return the previous Friday, we want to store the result
                //for this actual date, so that user is aware of the nature of the result
                result.date to (result.rates[symbol]
                    ?: throw IOException("Unable to retrieve data for $symbol"))
            }

            ExchangeRateApi.OpenExchangeRates -> {
                requireNotNull(apiKey)
                val result = (if (date < today) {
                    openExchangeRates.getHistorical(date, "$symbol,$base", apiKey)
                } else {
                    openExchangeRates.getLatest("$symbol,$base", apiKey)
                })
                val otherRate = result.rates[symbol]
                val baseRate = result.rates[base]
                if (otherRate != null && baseRate != null) {
                    //We ignore the time returned as part of the response, since it might relate to a different
                    //day then the requested one (due to time zone difference)
                    date to otherRate / baseRate
                } else throw IOException("Unable to retrieve data for $symbol")
            }

            ExchangeRateApi.CoinApi -> {
                requireNotNull(apiKey)
                val call = coinApi.getExchangeRate(base, symbol, date.takeIf { it < today }, apiKey)
                val result = call
                //We ignore the time returned as part of the response, since it might relate to a different
                //day then the requested one
                date to result.rate
            }
        }
    } catch (e: HttpException) {
        throw source.convertError(e)
    }

    /**
     * Load the value of 1 unit of base currency expressed in symbol for each date in the range
     */
    suspend fun getTimeSeries(
        source: ExchangeRateApi,
        apiKey: String?,
        start: LocalDate,
        end: LocalDate,
        base: String,
        symbol: String,
        except: Collection<LocalDate> = emptySet(),
    ): Pair<List<Pair<LocalDate, Double>>, Exception?> = try {
        require(start <= end)
        if (!source.isSupported(symbol, base)) {
            throw UnsupportedOperationException()
        }
        when (source) {
            ExchangeRateApi.Frankfurter -> {
                val result: Frankfurter.TimeSeriesResult = frankfurter.getTimeSeries(start, end, symbol, base)
                result.rates.map { (date, rates) ->
                    date to (rates[symbol]
                        ?: throw IOException("Unable to retrieve data for $symbol")
                    )
                } to null
            }

            ExchangeRateApi.OpenExchangeRates -> {
                requireNotNull(apiKey)

                val result = mutableMapOf<LocalDate, Double>()
                var exception: Exception? = null
                getDatesBetween(start, end, except).forEach { date ->
                    try {
                        result += getRate(source, apiKey, date, base, symbol)
                    } catch (e: Exception) {
                        exception = e
                        return@forEach
                    }
                }
                result.toList() to exception
            }

            ExchangeRateApi.CoinApi -> {
                requireNotNull(apiKey)
                val timeStart = "${start}T00:00:00"
                val timeEnd = "${end}T23:59:59"
                coinApi.getHistory(base, symbol, timeStart, timeEnd, apiKey).map { (date, rate) ->
                    date.toLocalDate() to rate
                } to null
            }
        }
    } catch (e: HttpException) {
        throw source.convertError(e)
    }
}

// Helper function to get all dates in a range (inclusive)
fun getDatesBetween(startDate: LocalDate, endDate: LocalDate, except: Collection<LocalDate>): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        if (currentDate !in except) {
            dates.add(currentDate)
        }
        currentDate = currentDate.plusDays(1)
    }
    return dates
}