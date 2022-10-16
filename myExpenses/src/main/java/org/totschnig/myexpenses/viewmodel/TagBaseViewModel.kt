package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import org.totschnig.myexpenses.viewmodel.data.Tag

open class TagBaseViewModel(application: Application,
                            val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {
    protected val tagsInternal = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>> = tagsInternal

    fun addDeletedTagId(tagId: Long) {
        savedStateHandle[KEY_DELETED_IDS] = longArrayOf(*getDeletedTagIds(), tagId)
    }

    fun getDeletedTagIds(): LongArray {
        return savedStateHandle.get<LongArray>(KEY_DELETED_IDS) ?: LongArray(0)
    }

    companion object {
        const val KEY_DELETED_IDS = "deletedIds"
    }
}