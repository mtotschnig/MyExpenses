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
import eltos.simpledialogfragment.form.ColorField
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelection
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelectionArgs
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelectionArgsV2
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment.Companion.accountSelectionV2
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.KEY_ACCOUNT_GROUPING
import org.totschnig.myexpenses.model.KEY_ACCOUNT_GROUPING_GROUP
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.util.toggle
import org.totschnig.myexpenses.viewmodel.data.Tag

class TagListViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    TagBaseViewModel(application, savedStateHandle) {

    private val tagsInternal = MutableLiveData<List<Tag>>()
    val tags: LiveData<List<Tag>> = tagsInternal

    fun toggleSelectedTagId(tagId: Long) {
        savedStateHandle[KEY_SELECTED_IDS] = selectedTagIds.apply {
            toggle(tagId)
        }
    }

    var selectedTagIds: MutableSet<Long>
        get() = savedStateHandle[KEY_SELECTED_IDS] ?: mutableSetOf()
        set(value) {
            savedStateHandle[KEY_SELECTED_IDS] = value
        }

    fun loadTags() {
        viewModelScope.launch {
            val accountId = savedStateHandle.get<Long>(KEY_ACCOUNTID)
            val isV2 = savedStateHandle.contains(KEY_ACCOUNT_GROUPING)
            val builder = TransactionProvider.TAGS_URI.buildUpon()

            if (accountId == null && !isV2) {
                builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_COUNT)
            } else {
                builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_FILTER)
            }

            val (accountSelection, accountSelectionArgs) = if (accountId == null) {
                if (isV2) {
                    val accountGrouping =
                        AccountGrouping.valueOf(savedStateHandle[KEY_ACCOUNT_GROUPING]!!)
                    val group = savedStateHandle.get<String>(KEY_ACCOUNT_GROUPING_GROUP)!!
                    accountSelectionV2(accountGrouping) to
                            accountSelectionArgsV2(accountGrouping, group)
                } else null to null
            } else {
                (accountSelection(accountId).takeIf { it.isNotEmpty() }
                    ?: "$KEY_ACCOUNTID IS NOT NULL") to
                        accountSelectionArgs(accountId)
            }

            contentResolver.observeQuery(
                uri = builder.build(),
                sortOrder = "$KEY_LABEL COLLATE $collate",
                selection = accountSelection,
                selectionArgs = accountSelectionArgs,
                notifyForDescendants = true
            ).mapToList(mapper = Tag.Companion::fromCursor)
                .collect(tagsInternal::postValue)
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
            contentResolver.insert(
                TransactionProvider.TAGS_URI,
                ContentValues().apply { put(KEY_LABEL, label) })?.let {
                ContentUris.parseId(it)
            }?.let {
                toggleSelectedTagId(it)
                emit(Tag(it, label))
            }
        }

    fun updateTag(tag: Tag, newLabel: String, color: Int) =
        liveData(context = coroutineContext()) {
            emit(
                try {
                    contentResolver.update(
                        ContentUris.withAppendedId(
                            TransactionProvider.TAGS_URI,
                            tag.id
                        ),
                        ContentValues().apply {
                            put(KEY_LABEL, newLabel)
                            if (color != ColorField.NONE) {
                                put(KEY_COLOR, color)
                            } else {
                                putNull(KEY_COLOR)
                            }
                        }, null, null
                    )
                } catch (_: SQLiteConstraintException) {
                    0
                } == 1
            )
        }

    companion object {
        const val KEY_SELECTED_IDS = "selectedIds"
    }
}