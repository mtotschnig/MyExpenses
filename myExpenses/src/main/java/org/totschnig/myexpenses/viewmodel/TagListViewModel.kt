package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.apache.commons.collections4.ListUtils
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.viewmodel.data.Tag

class TagListViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    TagBaseViewModel(application, savedStateHandle) {
    private val tags = MutableLiveData<List<Tag>>()

    fun loadTags(selected: List<Tag>?): LiveData<List<Tag>> {
        if (tags.value == null) {
            val tagsUri = TransactionProvider.TAGS_URI.buildUpon()
                .appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_COUNT).build()
            disposable = briteContentResolver.createQuery(
                tagsUri,
                null,
                null,
                null,
                "$KEY_LABEL COLLATE LOCALIZED",
                false
            )
                .mapToList { cursor ->
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
                    val label = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL))
                    val count = cursor.getColumnIndex(KEY_COUNT).takeIf { it > -1 }
                        ?.let { cursor.getInt(it) }
                        ?: -1
                    Tag(id, label, selected?.find { tag -> tag.label == label } != null, count)
                }
                .subscribe { list ->
                    tags.postValue(selected?.let {
                        ListUtils.union(
                            it.filter { tag -> tag.id == -1L },
                            list
                        )
                    }
                        ?: list)
                    dispose()
                }
        }
        return tags
    }

    fun removeTagAndPersist(tag: Tag): LiveData<Boolean> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val result = contentResolver.delete(
                ContentUris.withAppendedId(
                    TransactionProvider.TAGS_URI,
                    tag.id
                ), null, null
            )
            val success = result == 1
            if (success) {
                removeTag(tag)
                addDeletedTagId(tag.id)
            }
            emit(success)
        }

    private fun removeTag(tag: Tag) {
        tags.postValue(tags.value?.filter { it != tag })
    }

    fun addTagAndPersist(label: String): LiveData<Boolean> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val result = contentResolver.insert(TransactionProvider.TAGS_URI,
                ContentValues().apply { put(KEY_LABEL, label) })
            val success = result?.let {
                tags.postValue(buildList {
                    Tag(ContentUris.parseId(it), label, true)
                    tags.value?.let { addAll(it) }
                })
                true
            } ?: false
            emit(success)
        }

    fun updateTag(tag: Tag, newLabel: String) =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val result = try {
                contentResolver.update(ContentUris.withAppendedId(
                    TransactionProvider.TAGS_URI,
                    tag.id
                ),
                    ContentValues().apply { put(KEY_LABEL, newLabel) }, null, null
                )
            } catch (e: SQLiteConstraintException) {
                0
            }
            val success = result == 1
            if (success) {
                tags.postValue(tags.value?.map {
                    if (it == tag) Tag(
                        tag.id,
                        newLabel,
                        tag.selected,
                        tag.count
                    ) else it
                })
            }
            emit(success)
        }
}