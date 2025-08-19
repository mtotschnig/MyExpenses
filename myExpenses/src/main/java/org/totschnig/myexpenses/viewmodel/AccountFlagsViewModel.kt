package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.addAccountFlag
import org.totschnig.myexpenses.db2.deleteAccountFlag
import org.totschnig.myexpenses.db2.getAccountFlags
import org.totschnig.myexpenses.db2.setAccountFlagVisible
import org.totschnig.myexpenses.db2.updateAccountFlag
import org.totschnig.myexpenses.model.AccountFlag

data class AccountFlagsUiState(
    val isLoading: Boolean = false,
    val accountFlags: List<AccountFlag> = emptyList(),
    val editingAccountFlag: AccountFlag? = null // For editing an existing one
)

class AccountFlagsViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val editingAccountFLag = MutableStateFlow<AccountFlag?>(null)

    val uiState: StateFlow<AccountFlagsUiState> by lazy {
        combine(repository.getAccountFlags(), editingAccountFLag) { data, edit ->
            AccountFlagsUiState(
                accountFlags = data.sortedByDescending { it.sortKey },
                editingAccountFlag = edit
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, AccountFlagsUiState(isLoading = true))
    }

    fun onAdd() {
        editingAccountFLag.update {
            AccountFlag(label = "", sortKey = 0)
        }
    }

    fun onEdit(accountType: AccountFlag) {
        editingAccountFLag.update { accountType }
    }

    fun onDelete(accountFlag: AccountFlag) {
        viewModelScope.launch(coroutineDispatcher) {
            repository.deleteAccountFlag(accountFlag.id)
        }
    }

    fun onDialogDismiss() {
        editingAccountFLag.update { null }
    }

    fun onToggleVisibility(
        id: Long,
        visible: Boolean
    ) {
        viewModelScope.launch(coroutineDispatcher) {
            repository.setAccountFlagVisible(id, visible)
        }
    }

    fun onSave(
        label: String,
        icon: String?
    ) {
        val currentEditingFlag = editingAccountFLag.value ?: return
        viewModelScope.launch(coroutineDispatcher) {

            if (currentEditingFlag.id > 0) {
                repository.updateAccountFlag(
                    currentEditingFlag.copy(
                        label = label,
                        icon = icon
                    )
                )
            } else {
                // Add new
                val newFlag = AccountFlag(
                    label = label,
                    icon = icon,
                    sortKey = 0
                )
                repository.addAccountFlag(newFlag)
            }
            onDialogDismiss()
        }
    }
}