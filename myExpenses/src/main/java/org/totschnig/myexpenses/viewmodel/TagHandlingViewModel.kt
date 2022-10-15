package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import org.totschnig.myexpenses.viewmodel.data.Tag

open class TagHandlingViewModel(application: Application, savedStateHandle: SavedStateHandle)
    : TagBaseViewModel(application, savedStateHandle) {
    protected var userHasUpdatedTags = false

    fun updateTags(tagList: List<Tag>, fromUser: Boolean) {
        if (fromUser) {
            userHasUpdatedTags = true
        }
        tagsInternal.postValue(tagList)
    }

    fun removeTag(tag: Tag) {
        userHasUpdatedTags = true
        tagsInternal.value = tagsInternal.value?.minus(tag)
    }

    fun removeTags(tagIds: LongArray) {
        tagsInternal.value?.let { tagsInternal.postValue(it.filter { tag -> !tagIds.contains(tag.id) }) }
    }
}