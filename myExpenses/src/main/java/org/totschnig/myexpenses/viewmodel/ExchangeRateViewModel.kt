package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment.Companion.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.retrofit.MissingApiKeyException
import timber.log.Timber
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

    private fun loadFromDb(
        base: String,
        other: String,
        date: LocalDate,
        source: ExchangeRateSource,
    ) = contentResolver.query(
        TransactionProvider.PRICES_URI,
        arrayOf(KEY_VALUE),
        "$KEY_CURRENCY = ? AND $KEY_COMMODITY = ? AND $KEY_DATE = ? AND $KEY_SOURCE = ?",
        arrayOf(base, other, date.toString(), source.name),
        null, null
    )?.use {
        if (it.moveToFirst()) it.getDouble(0) else null
    }

    /**
     * Load the value of 1 unit of other currency expressed in base currency
     */
    suspend fun loadExchangeRate(
        other: String,
        base: String,
        date: LocalDate,
        source: ExchangeRateSource,
    ): Double = withContext(coroutineContext()) {
        if (date == LocalDate.now() && !source.limitToOneRequestPerDay) {
            loadFromNetwork(
                source = source,
                date = date,
                other = other,
                base = base
            ).second
        } else loadFromDb(base, other, date, source)
            ?: loadFromNetwork(source, date, other, base).second
    }

    suspend fun loadFromNetwork(
        source: ExchangeRateSource,
        date: LocalDate,
        other: String,
        base: String,
    ) = withContext(coroutineContext()) {
        exchangeRateService.getRate(
            source,
            (source as? ExchangeRateSource.SourceWithApiKey)?.requireApiKey(prefHandler),
            date,
            base,
            other
        ).also {
            Timber.d("loadFromNetwork: %s", it)
            repository.savePrice(base, other, it.first, source, it.second)
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
            context. getString(R.string.pref_exchange_rates_api_key_summary, source.host)
        )

    else -> this
}