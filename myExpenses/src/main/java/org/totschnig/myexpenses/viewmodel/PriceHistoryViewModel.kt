package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.core.os.BundleCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
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
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.viewmodel.data.Price
import java.math.BigDecimal
import java.time.LocalDate

class PriceHistoryViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ExchangeRateViewModel(application) {

    val commodity: String
        get() = savedStateHandle.get<String>(KEY_COMMODITY)!!

    private val inverseRatePreferenceKey = booleanPreferencesKey("inverseRate_$commodity")

    val inverseRate: Flow<Boolean> by lazy {
        dataStore.data.map { preferences ->
            preferences[inverseRatePreferenceKey] == true
        }
    }

    suspend fun persistInverseRate(inverseRate: Boolean) {
        dataStore.edit { preference ->
            preference[inverseRatePreferenceKey] = inverseRate
        }
    }

    val homeCurrency
        get() = currencyContext.homeCurrencyString

    val relevantSources: List<ExchangeRateApi> by lazy {
        prefHandler.getString("${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${commodity}")
            ?.takeIf { it != SERVICE_DEACTIVATED }
            ?.let { listOf(ExchangeRateApi.getByName(it)) }
            ?: ExchangeRateApi.configuredSources(prefHandler).filter {
                it.isSupported(homeCurrency, commodity)
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
                value = calculateRealExchangeRate(it.getDouble(2), currencyContext[commodity], currencyContext.homeCurrencyUnit)
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
            find { it.date == date }
        }
    }

    fun deletePrice(price: Price) {
        repository.deletePrice(price.date, price.source, homeCurrency, commodity)
    }

    fun savePrice(date: LocalDate, value: BigDecimal) {
        repository.savePrice(
            currencyContext.homeCurrencyUnit,
            currencyContext[commodity],
            date,
            ExchangeRateSource.User,
            value
        )
    }
}