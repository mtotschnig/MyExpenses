package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.loadPrice
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.MissingApiKeyException
import timber.log.Timber
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

/**
 * We want to store the price of a foreign currency relative to our base currency
 * APIs usually have the opposite understanding of base. They express the value of 1 unit
 * of the base currency relative to other currency
 */
open class ExchangeRateViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var exchangeRateService: ExchangeRateService

    /**
     * Load the value of 1 unit of other currency expressed in base currency
     */
    suspend fun loadExchangeRate(
        other: CurrencyUnit,
        base: CurrencyUnit,
        date: LocalDate,
        source: ExchangeRateApi?,
    ): BigDecimal = withContext(coroutineContext()) {
        (if (source == null || date != LocalDate.now() || source.limitToOneRequestPerDay)
            repository.loadPrice(base, other, date, source)
        else null) ?: loadFromNetwork(source ?: bestSource(other.code) , date, other.code, base.code).second
    }

    private fun bestSource(currency: String): ExchangeRateApi {
        val configuredSources = ExchangeRateApi.configuredSources(prefHandler)
        return preferredSource(currency).takeIf { configuredSources.contains(it) } ?:
        configuredSources.firstOrNull {
                it.isSupported(currency)
            } ?: throw UnsupportedOperationException("No supported source for $currency")
    }

    //TODO: the source selected on the Price History could be considered preferred
    private fun preferredSource(currency: String): ExchangeRateApi? = null

    suspend fun loadFromNetwork(
        source: ExchangeRateApi,
        date: LocalDate,
        other: String,
        base: String,
    ) = withContext(coroutineContext()) {
        exchangeRateService.getRate(
            source,
            (source as? ExchangeRateApi.SourceWithApiKey)?.requireApiKey(prefHandler),
            date,
            base,
            other
        ).let { it.first to BigDecimal.valueOf(it.second) }
            .also {
                Timber.d("loadFromNetwork: %s", it)
                repository.savePrice(
                    currencyContext[base],
                    currencyContext[other],
                    it.first,
                    source,
                    it.second
                )
            }
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