package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.insertCurrency
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Currency
import java.text.Collator

open class CurrencyViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    /**
     * sorted by usages, code
     */
    val currencyUnits: Flow<List<CurrencyUnit>>
        get() = currencyContext.getAll()

    val currencies: Flow<List<Currency>>
        get() = contentResolver.observeQuery(
            TransactionProvider.CURRENCIES_URI, null, null, null,
            KEY_CODE, true
        )
            .mapToList(dispatcher = coroutineDispatcher) {
                Currency.create(
                    it,
                    userPreferredLocale
                )
            }
            .map { it.sorted() }

    val usedCurrencies: StateFlow<List<CurrencyUnit>> by lazy {
        contentResolver.observeQuery(
            TransactionProvider.DYNAMIC_CURRENCIES_URI,
        ).mapToList(dispatcher = coroutineDispatcher) { currencyContext[it.getString(0)] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribedWithTimeout, emptyList())
    }

    val currenciesFromEnum: List<Currency>
        get() = CurrencyEnum.entries
            .map { Currency.create(it.name, userPreferredLocale) }
            .sorted()

    private fun List<Currency>.sorted(): List<Currency> = try {
        Collator.getInstance()
    } catch (e: Exception) {
        CrashHandler.report(e)
        null
    }?.let { collator ->
        this.sortedWith { lhs, rhs ->
            rhs.usages.compareTo(lhs.usages).takeIf { it != 0 }
                ?: lhs.sortClass.compareTo(rhs.sortClass).takeIf { it != 0 }
                ?: collator.compare(lhs.toString(), rhs.toString())
        }
    } ?: this

    val default: Currency
        get() = Currency.create(
            currencyContext.homeCurrencyString,
            userPreferredLocale
        )

    fun createAsset(
        asset: Currency
    ) {
        viewModelScope.launch(coroutineDispatcher) {
            with(asset) {
                repository.insertCurrency(code, symbol, displayName, fractionDigits, commodityType)
            }
        }
    }
}
