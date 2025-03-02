package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.totschnig.myexpenses.R

interface CompletedAction {
    val label: Int
}

object CloseAction: CompletedAction {
    override val label: Int = R.string.menu_close
}

sealed class TargetAction(
    override val label: Int,
    val mimeType: String,
    val targets: List<Uri>,
    val bulk: Boolean
) : CompletedAction

class ShareAction(
    mimeType: String,
    targets: List<Uri>
) : TargetAction(R.string.share, mimeType, targets, true)

class OpenAction(
    mimeType: String,
    targets: List<Uri>
) : TargetAction(R.string.menu_open, mimeType, targets, false)

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