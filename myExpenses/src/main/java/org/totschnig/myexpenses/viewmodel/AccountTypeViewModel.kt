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
import org.totschnig.myexpenses.db2.addAccountType
import org.totschnig.myexpenses.db2.deleteAccountType
import org.totschnig.myexpenses.db2.getAccountTypes
import org.totschnig.myexpenses.db2.saveAccountTypeOrder
import org.totschnig.myexpenses.db2.updateAccountType
import org.totschnig.myexpenses.model.AccountType

data class AccountTypesUiState(
    val isLoading: Boolean = false,
    val accountTypes: List<AccountType> = emptyList(),
    val editingAccountType: AccountType? = null // For editing an existing one
)

class AccountTypeViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val editingAccountType = MutableStateFlow<AccountType?>(null)

    val uiState: StateFlow<AccountTypesUiState> by lazy {
        combine(repository.getAccountTypes(), editingAccountType) { accountTypes, editingAccountType ->
            AccountTypesUiState(
                accountTypes = accountTypes,
                editingAccountType = editingAccountType
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, AccountTypesUiState(isLoading = true))
    }

    fun onAdd() {
        editingAccountType.update {
            AccountType(name = "")
        }
    }

    fun onEdit(accountType: AccountType) {
        editingAccountType.update { accountType }
    }

    fun onDialogDismiss() {
        editingAccountType.update { null }
    }

    fun onSave(
        name: String,
        isAsset: Boolean,
        supportsReconciliation: Boolean
    ) {
        val currentEditingType = editingAccountType.value ?: return
        viewModelScope.launch(coroutineDispatcher) {

            if (currentEditingType.id > 0) {
                // Update existing
                val updatedType = currentEditingType.copy(
                    name = name,
                    isAsset = isAsset,
                    supportsReconciliation = supportsReconciliation
                )
                repository.updateAccountType(updatedType)
            } else {
                // Add new
                val newType = AccountType(
                    name = name,
                    isAsset = isAsset,
                    supportsReconciliation = supportsReconciliation
                )
                repository.addAccountType(newType)
            }
            onDialogDismiss()
        }
    }

    fun onDelete(accountType: AccountType) {
        viewModelScope.launch(coroutineDispatcher) {
            repository.deleteAccountType(accountType.id)
        }
    }

    fun onSortOrderConfirmed(sortedIds: LongArray) {
        repository.saveAccountTypeOrder(sortedIds)
    }
}
