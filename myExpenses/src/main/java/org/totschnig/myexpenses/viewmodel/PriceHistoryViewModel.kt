package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.db2.deletePrice
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.preference.PrefHandler.Companion.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX
import org.totschnig.myexpenses.preference.PrefHandler.Companion.SERVICE_DEACTIVATED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.provider.mapToListWithExtra
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import java.time.LocalDate

data class Price(val date: LocalDate, val source: ExchangeRateSource?, val value: Double)

class PriceHistoryViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ExchangeRateViewModel(application) {

    val commodity: String
        get() = savedStateHandle.get<String>(KEY_COMMODITY)!!

    val relevantSources: List<ExchangeRateSource> by lazy {
        prefHandler.getString("${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${commodity}")
            ?.takeIf { it != SERVICE_DEACTIVATED }
            ?.let { listOf(ExchangeRateSource.getByName(it)!!) }
            ?: ExchangeRateSource.configuredSources(prefHandler).filter {
                it.isSupported(currencyContext.homeCurrencyString, commodity)
            }.also {
                if (it.size > 1) {
                    userSelectedSource = it[0]
                }
            }
    }

    var userSelectedSource: ExchangeRateSource? = null

    val effectiveSource: ExchangeRateSource?
        get() = when(relevantSources.size) {
            0 -> null
            1 -> relevantSources.first()
            else -> userSelectedSource
        }

    val prices by lazy {
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
            // User provided rates have priority
            this.sortedBy { if(it.source == null) 0 else 1 } .find { it.date == date }
        }
    }

    fun deletePrice(price: Price) {
        repository.deletePrice(price.date, price.source)
    }

    fun savePrice(date: LocalDate, value: Double) {
        repository.savePrice(currencyContext.homeCurrencyString, commodity, date, null, value)
    }
}