package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.deleteCurrency
import org.totschnig.myexpenses.db2.insertCurrency
import org.totschnig.myexpenses.db2.updateCurrencyLabel
import org.totschnig.myexpenses.db2.updateFractionDigits
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.util.ICurrencyFormatter
import javax.inject.Inject

class EditCurrencyViewModel(application: Application) : CurrencyViewModel(application) {
    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    private val _updateComplete = MutableStateFlow<Int?>(null)
    val updateComplete: StateFlow<Int?> = _updateComplete.asStateFlow()

    private val _insertComplete = MutableStateFlow<Boolean?>(null)
    val insertComplete: StateFlow<Boolean?> = _insertComplete.asStateFlow()

    private val _deleteComplete = MutableStateFlow<Boolean?>(null)
    val deleteComplete: StateFlow<Boolean?> = _deleteComplete.asStateFlow()

    fun save(
        currency: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        withUpdate: Boolean
    ) {
        viewModelScope.launch {
            var updatedAccounts: Int? = null
            currencyContext.storeCustomSymbol(currency, symbol)
            if (withUpdate) {
                updatedAccounts = repository.updateFractionDigits(currency, fractionDigits)
            } else {
                currencyContext.storeCustomFractionDigits(currency, fractionDigits)
            }
            if (label != null) {
                repository.updateCurrencyLabel(currency, label)
            }
            currencyFormatter.invalidate(
                getApplication<Application>().contentResolver,
                currency
            )
            _updateComplete.value = updatedAccounts ?: 0
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
            val uri = repository.insertCurrency(code, symbol, label, fractionDigits, commodityType)
            val success = uri != null
            if (success) {
                currencyContext.storeCustomSymbol(code, symbol)
                currencyContext.storeCustomFractionDigits(code, fractionDigits)
            }
            _insertComplete.value = success
        }
    }


    fun deleteCurrency(currency: String?) {
        currency?.let {
            viewModelScope.launch {
                val result = repository.deleteCurrency(it)
                _deleteComplete.value = result == 1
            }
        }
    }

    fun resetResults() {
        _updateComplete.value = null
        _insertComplete.value = null
        _deleteComplete.value = null
    }
}
