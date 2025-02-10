package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment.Companion.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.retrofit.ExchangeRateService
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * We want to store the price of a foreign currency relative to our base currency
 * APIs usually have the opposite understanding of base. They express the value of 1 unit
 * of the base currency relative to other currency
 */
class ExchangeRateViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    private fun loadFromDb(
        base: String,
        other: String,
        date: LocalDate,
        source: ExchangeRateSource,
    ) = contentResolver.query(
        TransactionProvider.PRICES_URI,
        arrayOf(KEY_VALUE),
        "$KEY_CURRENCY = ? AND $KEY_COMMODITY = ? AND $KEY_DATE = ? AND $KEY_SOURCE = ?",
        arrayOf(base, other, date.toString(), source.id),
        null, null
    )?.use {
        if (it.moveToFirst()) it.getDouble(0) else null
    }

    private fun storeInDb(
        base: String,
        other: String,
        date: LocalDate,
        rate: Double,
        source: ExchangeRateSource,
    ) {
        contentResolver.insert(TransactionProvider.PRICES_URI, ContentValues().apply {
            put(KEY_CURRENCY, base)
            put(KEY_COMMODITY, other)
            put(KEY_DATE, date.toString())
            put(KEY_SOURCE, source.id)
            put(KEY_VALUE, rate)
        })
    }

    @Inject
    lateinit var exchangeRateService: ExchangeRateService

    /**
     * Load the value of 1 unit of other currency expressed in base currency
     */
    suspend fun loadExchangeRate(
        other: String,
        base: String,
        date: LocalDate,
        source: ExchangeRateSource,
    ): Double {
        val apiKey = (source as? ExchangeRateSource.SourceWithApiKey)?.requireApiKey(prefHandler)
        return if (date == LocalDate.now() && !source.limitToOneRequestPerDay) {
            loadFromNetwork(
                source = source,
                apiKey = apiKey,
                date = date,
                other = other,
                base = base
            )
        } else loadFromDb(base, other, date, source)
            ?: loadFromNetwork(source, apiKey, date, other, base)
    }


    private suspend fun loadFromNetwork(
        source: ExchangeRateSource,
        apiKey: String?,
        date: LocalDate,
        other: String,
        base: String,
    ) = exchangeRateService.getRate(source, apiKey, date, base, other).also {
        Timber.d("loadFromNetwork: %s", it)
        storeInDb(base = base, other = other, date = it.first, rate = it.second, source = source)
    }.second

}