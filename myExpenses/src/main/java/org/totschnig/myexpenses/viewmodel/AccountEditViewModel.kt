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

    fun save(account: Account): LiveData<Result<Long>> = liveData(context = coroutineContext()) {
        emit(kotlin.runCatching {
            val id = if (account.id == 0L) {
                repository.createAccount(account)
            } else {
                repository.updateAccount(account.id, account.toContentValues())
                account
            }.id
            repository.saveActiveTagsForAccount(tagsLiveData.value, id)
            id
        })
    }
}