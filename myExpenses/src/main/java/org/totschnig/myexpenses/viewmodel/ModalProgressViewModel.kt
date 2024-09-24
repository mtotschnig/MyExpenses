package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ModalProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val _completed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed
    private val _message: MutableStateFlow<String> = MutableStateFlow("")
    val message: StateFlow<String> = _message

    fun appendToMessage(message: String) {
        _message.update {
            if (it.isEmpty()) message else it + "\n" + message
        }
    }

    fun onTaskCompleted() {
        _completed.update {
            true
        }
    }
}