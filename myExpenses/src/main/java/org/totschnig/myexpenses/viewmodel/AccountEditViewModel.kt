package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Tag

class AccountEditViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val tags = MutableLiveData<List<Tag>>()

    fun getTags(): LiveData<List<Tag>> {
        return tags
    }

    fun accountWithTags (Id: Long) : LiveData<Account?> = liveData(context = coroutineContext()) {
        Account.getInstanceFromDbWithTags(Id)?.also { pair ->
            emit(pair.first)
            pair.second?.takeIf { it.size > 0 }?.let { tags.postValue(it.toMutableList()) }
        }
    }

    fun save (account: Account) : LiveData<Long> = liveData(context = coroutineContext()) {
        val result = try {
            account.save()?.let { ContentUris.parseId(it) } ?: ERROR_UNKNOWN
        } catch (e: Exception) {
            CrashHandler.report(e)
            ERROR_UNKNOWN
        }
        emit(if (result > 0 && !account.saveTags(tags.value)) ERROR_WHILE_SAVING_TAGS else result)
    }

    fun updateTags(it: MutableList<Tag>) {
        tags.postValue(it)
    }

    fun removeTag(tag: Tag) {
        tags.value = tags.value?.minus(tag)
    }

    fun removeTags(tagIds: LongArray) {
        tags.value?.let { tags.postValue(it.filter { tag -> !tagIds.contains(tag.id) }.toMutableList()) }
    }

}