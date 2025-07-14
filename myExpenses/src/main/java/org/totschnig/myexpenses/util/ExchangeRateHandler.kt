package org.totschnig.myexpenses.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.loadPrice
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefHandler.Companion.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX
import org.totschnig.myexpenses.preference.PrefHandler.Companion.SERVICE_DEACTIVATED
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.MissingApiKeyException
import java.math.BigDecimal
import java.time.LocalDate

/**
 * We want to store the price of a foreign currency relative to our base currency
 * APIs usually have the opposite understanding of base. They express the value of 1 unit
 * of the base currency relative to other currency
 */
class ExchangeRateHandler(
    val exchangeRateService: ExchangeRateService,
    val repository: Repository,
    val prefHandler: PrefHandler,
    val currencyContext: CurrencyContext,
) {

    /**
     * Load the value of 1 unit of other currency expressed in base currency
     */
    suspend fun loadExchangeRate(
        other: CurrencyUnit,
        base: CurrencyUnit,
        date: LocalDate,
        source: ExchangeRateApi? = null,
    ): BigDecimal = withContext(Dispatchers.IO) {
        (if (source == null || date != LocalDate.now() || source.limitToOneRequestPerDay)
            repository.loadPrice(base, other, date, source)
        else null) ?: loadFromNetwork(
            source ?: bestSource(other.code),
            date,
            other.code,
            base.code
        ).second
    }

    private fun bestSource(currency: String) =
        relevantSources(currency)
            .firstOrNull()
            ?: throw UnsupportedOperationException("No supported source for $currency")

    fun relevantSources(commodity: String) = prefHandler.getString("${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${commodity}")
        ?.takeIf { it != SERVICE_DEACTIVATED }
        ?.let { listOf(ExchangeRateApi.getByName(it)) }
        ?: ExchangeRateApi.configuredSources(prefHandler).filter {
            it.isSupported(currencyContext.homeCurrencyString, commodity)
        }

    suspend fun loadFromNetwork(
        source: ExchangeRateApi,
        date: LocalDate,
        other: String,
        base: String,
    ) = withContext(Dispatchers.IO) {
        exchangeRateService.getRate(
            source,
            (source as? ExchangeRateApi.SourceWithApiKey)?.requireApiKey(prefHandler),
            date,
            other,
            base
        ).let { it.first to BigDecimal.valueOf(it.second) }
            .also {
            repository.savePrice(
                currencyContext[base],
                currencyContext[other],
                it.first,
                source,
                it.second
            )
        }
    }

    suspend fun loadTimeSeries(
        source: ExchangeRateApi,
        start: LocalDate,
        end: LocalDate,
        except: Set<LocalDate>,
        other: String,
        base: String,
    ): Pair<Int, Exception?> = withContext(Dispatchers.IO) {
        val (list, exception) = exchangeRateService.getTimeSeries(
            source = source,
            apiKey = (source as? ExchangeRateApi.SourceWithApiKey)?.requireApiKey(prefHandler),
            start = start,
            end = end,
            except = except,
            base = other,
            symbol = base
        )
        list.forEach { (date, rate) ->
            repository.savePrice(
                currencyContext[base],
                currencyContext[other],
                date,
                source,
                BigDecimal.valueOf(rate),
            )
        }
        list.size to exception
    }

}

fun Throwable.transformForUser(context: Context, other: String, base: String) = when (this) {
    is java.lang.UnsupportedOperationException ->
        Exception(
            context.getString(R.string.exchange_rate_not_supported, other, base)
        )

    is MissingApiKeyException ->
        Exception(
            context.getString(R.string.pref_exchange_rates_api_key_summary, source.host)
        )

    else -> this
}