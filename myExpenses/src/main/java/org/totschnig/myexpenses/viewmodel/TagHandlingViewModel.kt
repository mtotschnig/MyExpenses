package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.viewmodel.data.Tag

open class TagHandlingViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    protected val tags = MutableLiveData<List<Tag>>()
    protected var userHasUpdatedTags = false

    fun getTags(): LiveData<List<Tag>> {
        return tags
    }

    fun updateTags(tagList: List<Tag>, fromUser: Boolean) {
        if (fromUser) {
            userHasUpdatedTags = true
        }
        tags.postValue(tagList)
    }

    fun removeTag(tag: Tag) {
        userHasUpdatedTags = true
        tags.value = tags.value?.minus(tag)
    }

    fun removeTags(tagIds: LongArray) {
        tags.value?.let { tags.postValue(it.filter { tag -> !tagIds.contains(tag.id) }) }
    }
}