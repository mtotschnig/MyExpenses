package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle

open class TagBaseViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {

    fun addDeletedTagId(tagId: Long) {
        deletedTagIds = longArrayOf(*deletedTagIds, tagId)
    }

    var deletedTagIds: LongArray
        get() = savedStateHandle.get<LongArray>(KEY_DELETED_IDS) ?: LongArray(0)
        set(value) {
            savedStateHandle[KEY_DELETED_IDS] = value
        }

    companion object {
        const val KEY_DELETED_IDS = "deletedIds"
    }
}