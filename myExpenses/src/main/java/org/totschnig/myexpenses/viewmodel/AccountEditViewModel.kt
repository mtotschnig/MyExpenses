package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.*
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account

private const val DATA_LOADED_KEY = "dataLoaded"
private const val SYNC_ACCOUNT_NAME_KEY = "syncAccountName"
private const val CURRENCY_UNIT_KEY = "currencyUnit"
private const val ACCOUNT_TYPE_KEY = "accountType"
private const val EXCLUDE_FROM_TOTALS_KEY = "excludeFromTotals"
private const val DYNAMIC_EXCHANGE_RATES_KEY = "dynamicExchangeRates"
private const val UUID_KEY = "uuid"

class AccountEditViewModel(application: Application, savedStateHandle: SavedStateHandle)
    : TagHandlingViewModel(application, savedStateHandle) {

    var dataLoaded: Boolean
        get() = savedStateHandle.get<Boolean>(DATA_LOADED_KEY) ?: false
        set(value) = savedStateHandle.set(DATA_LOADED_KEY, value)

    var syncAccountName: String?
        get() = savedStateHandle.get<String>(SYNC_ACCOUNT_NAME_KEY)
        set(value) = savedStateHandle.set(SYNC_ACCOUNT_NAME_KEY, value)

    var currencyUnit: CurrencyUnit?
        get() = savedStateHandle.get<CurrencyUnit>(CURRENCY_UNIT_KEY)
        set(value) = savedStateHandle.set(CURRENCY_UNIT_KEY, value)

    var accountType: Long
        get() = savedStateHandle.get<Long>(ACCOUNT_TYPE_KEY) ?: 0L
        set(value) = savedStateHandle.set(ACCOUNT_TYPE_KEY, value)

    var excludeFromTotals: Boolean
        get() = savedStateHandle.get<Boolean>(EXCLUDE_FROM_TOTALS_KEY) ?: false
        set(value) = savedStateHandle.set(EXCLUDE_FROM_TOTALS_KEY, value)

    fun toggleExcludeFromTotals() {
        excludeFromTotals = !excludeFromTotals
    }

    var dynamicExchangeRates: Boolean
        get() = savedStateHandle.get<Boolean>(DYNAMIC_EXCHANGE_RATES_KEY) ?: false
        set(value) = savedStateHandle.set(DYNAMIC_EXCHANGE_RATES_KEY, value)

    fun toggleDynamicExchangeRates() {
        dynamicExchangeRates = !dynamicExchangeRates
    }


    var uuid: String?
        get() = savedStateHandle.get<String>(UUID_KEY)
        set(value) = savedStateHandle.set(UUID_KEY, value)

    fun loadAccount(id: Long): LiveData<Account?> = liveData(context = coroutineContext()) {
        emit(repository.loadAccount(id))
    }

    fun loadTags(accountId: Long) {
        viewModelScope.launch(coroutineContext()) {
            updateTags(repository.loadActiveTagsForAccount(accountId), false)
        }
    }

    fun save(accountIn: Account): LiveData<Result<Pair<Long, String>>> = liveData(context = coroutineContext()) {
        emit(kotlin.runCatching {
            val account = if (accountIn.id == 0L) {
                accountIn.createIn(repository)
            } else {
                repository.updateAccount(accountIn.id, accountIn.toContentValues())
                accountIn
            }
            licenceHandler.updateNewAccountEnabled()
            updateTransferShortcut()
            repository.saveActiveTagsForAccount(tagsLiveData.value?.map { it.id }, account.id)
            val homeCurrency = currencyContext.homeCurrencyUnit
            if (account.currency != homeCurrency.code) {
                repository.storeExchangeRate(
                    account.id,
                    account.exchangeRate,
                    account.currency,
                    homeCurrency.code
                )
            }
            account.id to account.uuid!!
        })
    }
}