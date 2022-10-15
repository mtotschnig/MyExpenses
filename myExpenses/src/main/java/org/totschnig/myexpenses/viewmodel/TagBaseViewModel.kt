package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle

open class TagBaseViewModel(application: Application,
                            private val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {
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