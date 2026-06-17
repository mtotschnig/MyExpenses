package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.deleteCurrency
import org.totschnig.myexpenses.db2.insertCurrency
import org.totschnig.myexpenses.db2.updateCurrency
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
        id: Long,
        currency: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        withUpdate: Boolean,
        oldCode: String?
    ) {
        viewModelScope.launch {
            var updatedAccountsCount = 0
            repository.updateCurrency(id, currency, symbol, label, fractionDigits, oldCode)
            if (withUpdate) {
                updatedAccountsCount = repository.updateFractionDigits(currency, fractionDigits)
            }
            currencyFormatter.invalidate(
                getApplication<Application>().contentResolver,
                currency
            )
            _updateComplete.value = updatedAccountsCount
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
            _insertComplete.value = success
        }
    }


    fun deleteCurrency(currencyID: Long) {
        viewModelScope.launch {
            val result = repository.deleteCurrency(currencyID)
            _deleteComplete.value = result == 1
        }
    }

    fun resetResults() {
        _updateComplete.value = null
        _insertComplete.value = null
        _deleteComplete.value = null
    }
}
