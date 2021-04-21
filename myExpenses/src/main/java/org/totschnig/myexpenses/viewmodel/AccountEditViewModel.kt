package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.util.HashMap

class AccountEditViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    protected val tags = MutableLiveData<MutableList<Tag>>()

    fun getTags(): LiveData<MutableList<Tag>> {
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
        } catch (e: Transaction.ExternalStorageNotAvailableException) {
            ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE
        } catch (e: Transaction.UnknownPictureSaveException) {
            val customData = HashMap<String, String>()
            customData["pictureUri"] = e.pictureUri.toString()
            customData["homeUri"] = e.homeUri.toString()
            CrashHandler.report(e, customData)
            ERROR_PICTURE_SAVE_UNKNOWN
        } catch (e: Plan.CalendarIntegrationNotAvailableException) {
            ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE
        } catch (e: Exception) {
            CrashHandler.report(e)
            ERROR_UNKNOWN
        }
        emit(if (result > 0 && !account.saveTags(tags.value, getApplication<Application>().contentResolver)) ERROR_WHILE_SAVING_TAGS else result)
    }

    fun updateTags(it: MutableList<Tag>) {
        tags.postValue(it)
    }

    fun removeTag(tag: Tag) {
        tags.value?.remove(tag)
    }

    fun removeTags(tagIds: LongArray) {
        tags.value?.let { tags.postValue(it.filter { tag -> !tagIds.contains(tag.id) }.toMutableList()) }
    }

}