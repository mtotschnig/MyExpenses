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
import org.totschnig.myexpenses.dialog.select.DataHolder
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.viewmodel.data.Transaction2

sealed class LoadState {
    object Loading : LoadState()
    class Result(val items: List<DataHolder>) : LoadState()
}

class SelectFromTableViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

    private val internalState: MutableState<LoadState> = mutableStateOf(LoadState.Loading)

    val data: State<LoadState> = internalState

    @OptIn(SavedStateHandleSaveableApi::class)
    val selectionState: MutableState<List<DataHolder>> =
        savedStateHandle.saveable("selectionState") { mutableStateOf(emptyList()) }

    fun loadData(uri: Uri, column: String, selection: String?, selectionArgs: Array<String>?) {
        val projection = arrayOf(DatabaseConstants.KEY_ROWID, column)
        viewModelScope.launch(context = coroutineContext()) {
            internalState.value = LoadState.Result(
                items = contentResolver.query(uri, projection, selection, selectionArgs, null)
                    ?.use {
                        it.asSequence.map { cursor ->
                            DataHolder(cursor.getLong(0), cursor.getString(1))
                        }.toList()
                    } ?: emptyList()
            )
        }
    }
}