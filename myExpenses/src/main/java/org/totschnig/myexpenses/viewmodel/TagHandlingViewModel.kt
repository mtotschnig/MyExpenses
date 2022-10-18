package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import org.totschnig.myexpenses.viewmodel.data.Tag

open class TagHandlingViewModel(application: Application, savedStateHandle: SavedStateHandle)
    : TagBaseViewModel(application, savedStateHandle) {

    val tagsLiveData : MutableLiveData<List<Tag>> = savedStateHandle.getLiveData(KEY_MAPPED_TAGS)

    protected var userHasUpdatedTags: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_USER_HAS_UPDATED_TAG) == true
        set(value) {
            savedStateHandle[KEY_USER_HAS_UPDATED_TAG] = value
        }

    fun updateTags(tagList: List<Tag>, fromUser: Boolean) {
        if (fromUser) {
            userHasUpdatedTags = true
        }
        tagsLiveData.postValue(tagList)
    }

    fun removeTag(tag: Tag) {
        userHasUpdatedTags = true
        removeTags(tag.id)
    }

    fun removeTags(vararg tagIds: Long) {
        savedStateHandle[KEY_MAPPED_TAGS] = tagsLiveData.value?.filter { !tagIds.contains(it.id) }
    }

    companion object {
        private const val KEY_MAPPED_TAGS = "mappedTags"
        private const val KEY_USER_HAS_UPDATED_TAG = "userHasUpdatedTags"
    }
}