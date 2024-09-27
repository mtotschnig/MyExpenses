package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

sealed class CompletedAction(
    val label: String,
    val mimeType: String,
    val targets: List<Uri>,
    val bulk: Boolean
)

class ShareAction(
    label: String,
    mimeType: String,
    targets: List<Uri>
) : CompletedAction(label, mimeType, targets, true)

class OpenAction(
    label: String,
    mimeType: String,
    targets: List<Uri>
) : CompletedAction(label, mimeType, targets, false)

class ModalProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val _completed: MutableStateFlow<List<CompletedAction>?> = MutableStateFlow(null)
    val completed: StateFlow<List<CompletedAction>?> = _completed
    private val _message: MutableStateFlow<String> = MutableStateFlow("")
    val message: StateFlow<String> = _message

    fun appendToMessage(message: String) {
        _message.update {
            if (it.isEmpty()) message else it + "\n" + message
        }
    }

    fun onDismissMessage() {
        _message.update { "" }
        _completed.update { null }
    }

    fun onTaskCompleted(actions: List<CompletedAction>) {
        _completed.update { actions }
    }
}