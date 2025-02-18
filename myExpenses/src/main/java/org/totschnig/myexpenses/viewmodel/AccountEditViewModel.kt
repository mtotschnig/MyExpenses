package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.*
import org.totschnig.myexpenses.model2.Account

class AccountEditViewModel(application: Application, savedStateHandle: SavedStateHandle)
    : TagHandlingViewModel(application, savedStateHandle) {

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
            repository.saveActiveTagsForAccount(tagsLiveData.value, account.id)
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