package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.db2.deletePrice
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.preference.PrefHandler.Companion.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX
import org.totschnig.myexpenses.preference.PrefHandler.Companion.SERVICE_DEACTIVATED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONLY_MISSING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WITH_ACCOUNT_EXCHANGE_RATES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.provider.mapToListWithExtra
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import timber.log.Timber
import java.time.LocalDate

data class Price(val date: LocalDate, val source: ExchangeRateSource, val value: Double)

data class FullPrice(
    val date: LocalDate,
    val source: ExchangeRateSource,
    val value: Double,
    val currency: String,
    val commodity: String,
)

class PriceHistoryViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ExchangeRateViewModel(application) {

    val commodity: String
        get() = savedStateHandle.get<String>(KEY_COMMODITY)!!

    val relevantSources: List<ExchangeRateApi> by lazy {
        prefHandler.getString("${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${commodity}")
            ?.takeIf { it != SERVICE_DEACTIVATED }
            ?.let { listOf(ExchangeRateApi.getByName(it)) }
            ?: ExchangeRateApi.configuredSources(prefHandler).filter {
                it.isSupported(currencyContext.homeCurrencyString, commodity)
            }.also {
                if (it.size > 1) {
                    userSelectedSource = it[0]
                }
            }
    }

    var userSelectedSource: ExchangeRateApi? = null

    val effectiveSource: ExchangeRateApi?
        get() = when (relevantSources.size) {
            0 -> null
            1 -> relevantSources.first()
            else -> userSelectedSource
        }

    val pricesWithMissingDates by lazy {
        contentResolver.observeQuery(
            uri = TransactionProvider.PRICES_URI
                .buildUpon()
                .appendQueryParameter(KEY_COMMODITY, commodity)
                .build(),
            projection = arrayOf(KEY_DATE, KEY_SOURCE, KEY_VALUE),
            notifyForDescendants = true
        ).mapToListWithExtra {
            Price(
                date = it.getLocalDate(0),
                source = ExchangeRateSource.getByName(it.getString(1)),
                value = it.getDouble(2)
            )
        }
            .map {
                it.second.fillInMissingDates(
                    end = BundleCompat.getSerializable(
                        it.first,
                        KEY_MAX_VALUE,
                        LocalDate::class.java
                    )
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    }

    suspend fun reCalculatePrices(newHomeCurrency: String) = withContext(coroutineContext()) {

        var count = 0
        contentResolver.query(
            TransactionProvider.PRICES_URI,
            arrayOf(KEY_DATE, KEY_SOURCE, KEY_VALUE, KEY_CURRENCY, KEY_COMMODITY),
            null,
            null,
            null
        )
            ?.useAndMapToList {
                FullPrice(
                    date = it.getLocalDate(0),
                    source = ExchangeRateSource.getByName(it.getString(1)),
                    value = it.getDouble(2),
                    currency = it.getString(3),
                    commodity = it.getString(4)
                )
            }
            ?.groupBy { it.date }
            ?.forEach { date, prices ->
                Timber.d("Date %s", date)
                val existingPrices = mutableListOf<Pair<String, Double>>()
                existingPrices.addAll(prices.filter { it.currency == newHomeCurrency }
                    .map { it.commodity to it.value })
                Timber.d("Existing prices %s", existingPrices.joinToString())
                val pricesNotCalculated =
                    prices.filter { it.source != ExchangeRateSource.Calculation }
                // Go through all prices that we can just invert
                pricesNotCalculated.forEach { price ->
                    if (price.commodity == newHomeCurrency && existingPrices.none { it.first == price.currency }) {
                        val inverseValue = 1 / price.value
                        Timber.d(
                            "calculating %s:%s (from inverse): %f",
                            price.currency,
                            newHomeCurrency,
                            inverseValue
                        )
                        repository.savePrice(
                            newHomeCurrency,
                            price.currency,
                            date,
                            ExchangeRateSource.Calculation,
                            inverseValue
                        )
                        existingPrices.add(price.currency to inverseValue)
                        count++
                    }
                }
                pricesNotCalculated.forEach { price ->
                    if (price.commodity != newHomeCurrency && existingPrices.none { it.first == price.commodity }) {
                        existingPrices.find { it.first == price.currency }?.let { reference ->
                            val value = reference.second * price.value
                            //noinspection TimberArgCount
                            Timber.d(
                                "calculating %1\$s:%2\$s from (%1\$s:%3\$s and %3\$s:%2\$s): %4\$f",
                                price.commodity,
                                newHomeCurrency,
                                price.currency,
                                value
                            )
                            repository.savePrice(
                                newHomeCurrency,
                                price.commodity,
                                date,
                                ExchangeRateSource.Calculation,
                                value
                            )
                            existingPrices.add(price.commodity to value)
                            count++
                        }
                    }
                }
            }
        count
    }

    suspend fun reCalculateEquivalentAmounts(
        newHomeCurrency: String = currencyContext.homeCurrencyString,
        accountId: Long? = null,
        onlyMissing: Boolean = true,
        withAccountExchangeRates: Boolean = true
    ): Pair<Int, Int> =
        withContext(coroutineContext()) {
            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_RECALCULATE_EQUIVALENT_AMOUNTS, null,
                Bundle(1).apply {
                    putString(KEY_CURRENCY, newHomeCurrency)
                    accountId?.let { putLong(KEY_ACCOUNTID, it) }
                    putBoolean(KEY_ONLY_MISSING, onlyMissing)
                    putBoolean(KEY_WITH_ACCOUNT_EXCHANGE_RATES, withAccountExchangeRates)
                }
            )!!.getSerializable(TransactionProvider.KEY_RESULT) as Pair<Int, Int>
        }

    fun List<Price>.fillInMissingDates(
        start: LocalDate? = null,
        end: LocalDate? = null,
    ): Map<LocalDate, Price?> {
        val seed = start ?: LocalDate.now()

        val maxDate = listOfNotNull(end, minOfOrNull { it.date }).minOrNull() ?: seed
        val allDates = generateSequence(seed) { it.minusDays(1) }
            .takeWhile { it >= maxDate }
            .toList()

        return allDates.associateWithTo(LinkedHashMap()) { date ->
            // User provided rates have priority, then API, then Calculation
            this.sortedBy {
                when (it.source) {
                    ExchangeRateSource.User -> 0
                    ExchangeRateSource.Calculation -> 2
                    else -> 1
                }
            }.find { it.date == date }
        }
    }

    fun deletePrice(price: Price) {
        repository.deletePrice(price.date, price.source)
    }

    fun savePrice(date: LocalDate, value: Double) {
        repository.savePrice(
            currencyContext.homeCurrencyString,
            commodity,
            date,
            ExchangeRateSource.User,
            value
        )
    }
}