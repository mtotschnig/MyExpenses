package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.provider.mapToListWithExtra
import java.time.LocalDate

data class Price(val date: LocalDate, val source: String, val value: Double)

class HistoricPricesViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {
    val prices by lazy {
        contentResolver.observeQuery(
            uri = TransactionProvider.PRICES_URI
                .buildUpon()
                .appendQueryParameter(KEY_COMMODITY, savedStateHandle.get<String>(KEY_COMMODITY)!!)
                .build(),
            projection = arrayOf(KEY_DATE, KEY_SOURCE, KEY_VALUE),
        ).mapToListWithExtra { Price(it.getLocalDate(0), it.getString(1), it.getDouble(2)) }
            .map { it.second.fillInMissingDates(end = BundleCompat.getSerializable(it.first, KEY_MAX_VALUE, LocalDate::class.java)) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    }

    fun List<Price>.fillInMissingDates(
        start: LocalDate? = null,
        end: LocalDate? = null,
    ): Map<LocalDate, Price?> {
        val seed = start ?: LocalDate.now()
        val maxDate = end ?: minOfOrNull { it.date } ?: seed
        val allDates = generateSequence(seed) { it.minusDays(1) }
            .takeWhile { it >= maxDate }
            .toList()

        return allDates.associateWithTo(LinkedHashMap()) { date ->
            this.find { it.date == date }
        }
    }

}