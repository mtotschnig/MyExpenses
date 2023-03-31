package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.toContentValues
import org.totschnig.myexpenses.db2.updateAccount
import org.totschnig.myexpenses.model.loadTags
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider

class AccountEditViewModel(application: Application, savedStateHandle: SavedStateHandle)
    : TagHandlingViewModel(application, savedStateHandle) {

    fun loadAccount(id: Long): LiveData<Account?> = liveData(context = coroutineContext()) {
        emit(repository.loadAccount(id))
    }

    fun loadTags(accountId: Long) {
        viewModelScope.launch(coroutineContext()) {
            updateTags(
                loadTags(
                    TransactionProvider.ACCOUNTS_TAGS_URI,
                    DatabaseConstants.KEY_ACCOUNTID,
                    accountId,
                    contentResolver
                ), false
            )
        }
    }

    fun save(account: Account): LiveData<Result<Long>> = liveData(context = coroutineContext()) {
        emit(kotlin.runCatching {
            if (account.id == 0L) {
                repository.createAccount(account)
            } else {
                repository.updateAccount(account.id, account.toContentValues())
                account
            }.id
        })
        //emit(if (result > 0 && !account.saveTags(tagsLiveData.value, contentResolver)) ERROR_WHILE_SAVING_TAGS else result)
    }
}