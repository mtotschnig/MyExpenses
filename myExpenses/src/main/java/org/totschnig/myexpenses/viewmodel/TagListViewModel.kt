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
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.util.toggle
import org.totschnig.myexpenses.viewmodel.data.Tag

class TagListViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    TagBaseViewModel(application, savedStateHandle) {

    private val tagsInternal = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>> = tagsInternal

    fun toggleSelectedTagId(tagId: Long) {
        savedStateHandle[KEY_SELECTED_IDS] = selectedTagIds.toMutableSet().apply {
            toggle(tagId)
        }
    }

    var selectedTagIds: HashSet<Long>
        get() = savedStateHandle[KEY_SELECTED_IDS] ?: HashSet()
        set(value) {
            savedStateHandle[KEY_SELECTED_IDS] = value
        }

    fun loadTags() {
        viewModelScope.launch {
            val accountId = savedStateHandle.get<Long>(KEY_ACCOUNTID)
            val builder = TransactionProvider.TAGS_URI.buildUpon()

            if (accountId == null) {
                builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_COUNT)
            } else {
                builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_FILTER)
            }

            contentResolver.observeQuery(
                uri = builder.build(),
                sortOrder = "$KEY_LABEL COLLATE $collate",
                selection = if (accountId == null) null else
                    (SelectFromMappedTableDialogFragment.accountSelection(accountId)
                        ?: ("$KEY_ACCOUNTID IS NOT NULL")),
                selectionArgs = if (accountId == null) null else SelectFromMappedTableDialogFragment.accountSelectionArgs(accountId),
                notifyForDescendants = true
            ).mapToList { cursor ->
                Tag(
                    id = cursor.getLong(KEY_ROWID),
                    label = cursor.getString(KEY_LABEL),
                    count = cursor.getIntIfExists(KEY_COUNT) ?: -1
                )
            }.collect(tagsInternal::postValue)
        }
    }

    fun removeTagAndPersist(tag: Tag) {
        viewModelScope.launch(context = coroutineContext()) {
            if (contentResolver.delete(
                    ContentUris.withAppendedId(
                        TransactionProvider.TAGS_URI,
                        tag.id
                    ), null, null
                ) == 1
            ) {
                addDeletedTagId(tag.id)
            }
        }
    }

    fun addTagAndPersist(label: String): LiveData<Tag> =
        liveData(context = coroutineContext()) {
            contentResolver.insert(TransactionProvider.TAGS_URI,
                ContentValues().apply { put(KEY_LABEL, label) })?.let {
                ContentUris.parseId(it)
            }?.let {
                toggleSelectedTagId(it)
                emit(Tag(it, label))
            }
        }

    fun updateTag(tag: Tag, newLabel: String) =
        liveData(context = coroutineContext()) {
            val result = try {
                contentResolver.update(
                    ContentUris.withAppendedId(
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
                tagsInternal.postValue(tagsInternal.value?.map {
                    if (it == tag) Tag(
                        tag.id,
                        newLabel,
                        tag.count
                    ) else it
                })
            }
            emit(success)
        }

    companion object {
        const val KEY_SELECTED_IDS = "selectedIds"
    }
}