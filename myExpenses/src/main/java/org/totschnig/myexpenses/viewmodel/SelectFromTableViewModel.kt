package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.select.DataHolder
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID

sealed class LoadState {
    object Loading : LoadState()
    class Result(val items: List<DataHolder>) : LoadState()
}

class SelectFromTableViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

    private val internalState: MutableState<LoadState> = mutableStateOf(LoadState.Loading)

    val data: State<LoadState> = internalState

    val selection: List<DataHolder>?
        get() = (data.value as? LoadState.Result)?.items?.filter {
            selectionState.value.contains(it.id)
        }

    @OptIn(SavedStateHandleSaveableApi::class)
    val selectionState: MutableState<List<Long>> =
        savedStateHandle.saveable("selectionState") { mutableStateOf(emptyList()) }

    fun loadData(
        uri: Uri,
        column: String,
        selection: String?,
        selectionArgs: Array<String>?,
        withNullItem: Boolean
    ) {
        val projection = arrayOf(DatabaseConstants.KEY_ROWID, column)
        viewModelScope.launch(context = coroutineContext()) {
            internalState.value = LoadState.Result(
                items = buildList {
                    if (withNullItem) add(DataHolder(NULL_ITEM_ID, getString(R.string.unmapped)))
                    contentResolver.query(uri, projection, selection, selectionArgs, null)
                        ?.use {
                            it.asSequence.forEach { cursor ->
                                add(DataHolder.fromCursor(cursor,  column))
                            }
                        }
                }
            )
        }
    }
}