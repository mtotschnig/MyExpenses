package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.util.crashreporting.CrashHandler

class AccountEditViewModel(application: Application, savedStateHandle: SavedStateHandle)
    : TagHandlingViewModel(application, savedStateHandle) {

    fun accountWithTags(id: Long): LiveData<Account?> = liveData(context = coroutineContext()) {
        Account.getInstanceFromDbWithTags(id, contentResolver)?.also { pair ->
            emit(pair.first)
            pair.second?.takeIf { it.size > 0 }?.let { updateTags(it, false) }
        }
    }

    fun save(account: Account): LiveData<Long> = liveData(context = coroutineContext()) {
        val result = try {
            account.save(homeCurrencyProvider.homeCurrencyUnit)?.let { ContentUris.parseId(it) } ?: ERROR_UNKNOWN
        } catch (e: Exception) {
            CrashHandler.report(e)
            ERROR_UNKNOWN
        }
        emit(if (result > 0 && !account.saveTags(tagsLiveData.value, contentResolver)) ERROR_WHILE_SAVING_TAGS else result)
    }
}