package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.db2.deleteCurrency
import org.totschnig.myexpenses.db2.insertCurrency
import org.totschnig.myexpenses.db2.isCurrencyUsed
import org.totschnig.myexpenses.db2.updateCurrency
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyEnum
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Currency
import java.text.Collator
import javax.inject.Inject

open class CurrencyViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    private val _updateComplete = MutableStateFlow<Int?>(null)
    val updateComplete: StateFlow<Int?> = _updateComplete.asStateFlow()

    private val _insertComplete = MutableStateFlow<Boolean?>(null)
    val insertComplete: StateFlow<Boolean?> = _insertComplete.asStateFlow()

    private val _deleteComplete = MutableStateFlow<Boolean?>(null)
    val deleteComplete: StateFlow<Boolean?> = _deleteComplete.asStateFlow()

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

    val usedCurrencies: StateFlow<List<Pair<CurrencyUnit, CurrencyUnit>>> by lazy {
        contentResolver.observeQuery(
            TransactionProvider.DYNAMIC_CURRENCIES_URI,
        ).mapToList(dispatcher = coroutineDispatcher) {
            currencyContext[it.getString(0)] to currencyContext[it.getString(1)]
        }
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

    fun save(
        id: Long,
        currency: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        oldCode: String?,
        oldFractionDigits: Int?,
        commodityType: CommodityType?
    ) {
        viewModelScope.launch {
            val (_, accountsUpdated) = repository.updateCurrency(
                id, currency, symbol, label, fractionDigits, oldCode, commodityType, oldFractionDigits
            )
            currencyContext.invalidate(currency)
            currencyFormatter.invalidate(getApplication<Application>().contentResolver, currency)

            _updateComplete.value = if (oldFractionDigits != null) accountsUpdated else 0
        }
    }

    suspend fun createAsset(
        code: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        commodityType: CommodityType
    ): CurrencyUnit? = withContext(coroutineDispatcher) {
        val uri = repository.insertCurrency(code, symbol, label, fractionDigits, commodityType)
        uri?.let {
            val id = android.content.ContentUris.parseId(it)
            CurrencyUnit(
                code = code,
                symbol = symbol,
                fractionDigits = fractionDigits,
                description = label ?: code,
                commodityType = commodityType,
                databaseId = id
            )
        }
    }

    fun newCurrency(
        code: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        commodityType: CommodityType
    ) {
        viewModelScope.launch {
            val asset = createAsset(code, symbol, fractionDigits, label, commodityType)
            _insertComplete.value = asset != null
        }
    }

    fun deleteCurrency(currency: CurrencyUnit) {
        viewModelScope.launch {
            val result = repository.deleteCurrency(currency.code)
            currencyContext.invalidate(currency.code)
            _deleteComplete.value = result == 1
        }
    }

    fun resetResults() {
        _updateComplete.value = null
        _insertComplete.value = null
        _deleteComplete.value = null
    }

    suspend fun isCurrencyUsed(currencyCode: String): Boolean =
        repository.isCurrencyUsed(currencyCode)
}
