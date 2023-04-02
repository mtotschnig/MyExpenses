package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.*
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.util.calculateRawExchangeRate

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

    fun save(account: Account): LiveData<Result<Long>> = liveData(context = coroutineContext()) {
        emit(kotlin.runCatching {
            val id = if (account.id == 0L) {
                account.createIn(repository)
            } else {
                repository.updateAccount(account.id, account.toContentValues())
                account
            }.id
            licenceHandler.updateNewAccountEnabled()
            updateTransferShortcut()
            repository.saveActiveTagsForAccount(tagsLiveData.value, id)
            val homeCurrency = homeCurrencyProvider.homeCurrencyUnit
            if (account.currency != homeCurrency.code) {
                repository.storeExchangeRate(id,
                    calculateRawExchangeRate(
                        account.exchangeRate,
                        currencyContext[account.currency],
                        homeCurrency
                    ),
                    account.currency,
                    homeCurrency.code
                )
            }
            id
        })
    }
}