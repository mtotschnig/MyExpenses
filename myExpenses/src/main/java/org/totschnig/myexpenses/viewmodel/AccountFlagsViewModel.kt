package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.db2.addAccountFlag
import org.totschnig.myexpenses.db2.deleteAccountFlag
import org.totschnig.myexpenses.db2.getAccountFlags
import org.totschnig.myexpenses.db2.saveAccountFlagOrder
import org.totschnig.myexpenses.db2.saveSelectedAccountsForFlag
import org.totschnig.myexpenses.db2.setAccountFlagVisible
import org.totschnig.myexpenses.db2.updateAccountFlag
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.DEFAULT_FLAG_ID
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNT_FLAGS_URI
import org.totschnig.myexpenses.provider.triggerAccountListRefresh
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal

data class AccountForSelection(
    val id: Long,
    val label: String,
    val selected: Boolean
)

data class AccountFlagsUiState(
    val isLoading: Boolean = false,
    val accountFlags: List<AccountFlag> = emptyList(),
    val editingAccountFlag: AccountFlag? = null,
    val selectingAccountsForFlag: Pair<AccountFlag, List<AccountForSelection>>? = null
)

class AccountFlagsViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val editingAccountFLag = MutableStateFlow<AccountFlag?>(null)
    private val selectionAccountsForFlag = MutableStateFlow<Pair<AccountFlag, List<AccountForSelection>>?>(null)

    val uiState: StateFlow<AccountFlagsUiState> by lazy {
        combine(repository.getAccountFlags(), editingAccountFLag, selectionAccountsForFlag) { data, edit, selection ->
            AccountFlagsUiState(
                accountFlags = data,
                editingAccountFlag = edit,
                selectingAccountsForFlag = selection
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, AccountFlagsUiState(isLoading = true))
    }

    private val aggregateInvisibleKey: Preferences.Key<Boolean>
        get() = prefHandler.getBooleanPreferencesKey(PrefKey.INVISIBLE_ACCOUNTS_ARE_AGGREGATED)


    val aggregateInvisible: Flow<Boolean> by lazy {
        dataStore.data.map {
            it[aggregateInvisibleKey] != false
        }
    }

    fun persistAggregateInvisible(aggregate: Boolean) {
        viewModelScope.launch {
            dataStore.edit {
                it[aggregateInvisibleKey] = aggregate
            }
            contentResolver.triggerAccountListRefresh()
        }
    }

    fun onAdd() {
        editingAccountFLag.update {
            AccountFlag(label = "", sortKey = 0)
        }
    }

    fun onEdit(accountFlag: AccountFlag) {
        editingAccountFLag.update { accountFlag }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch(coroutineDispatcher) {
            repository.deleteAccountFlag(id)
        }
    }

    fun onDialogDismiss() {
        editingAccountFLag.update { null }
        selectionAccountsForFlag.update { null }
    }

    fun onToggleVisibility(
        id: Long,
        visible: Boolean
    ) {
        viewModelScope.launch(coroutineDispatcher) {
            repository.setAccountFlagVisible(id, visible)
        }
    }

    fun onStartSelection(flag: AccountFlag) {
        viewModelScope.launch(coroutineDispatcher) {
            val accounts: List<AccountMinimal> = accountsMinimal(
                query = "$KEY_FLAG IN ($DEFAULT_FLAG_ID, ?)",
                queryArgs = arrayOf(flag.id.toString()),
                withAggregates = false
            ).first()
            selectionAccountsForFlag.update {
                flag to accounts.map {
                    AccountForSelection(
                        id = it.id,
                        label = it.label,
                        selected = it.flag?.id == flag.id
                    )
                }
            }
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

    fun onSaveSelection(
        selected: Set<Long>
    ) {
        val currentSelection= selectionAccountsForFlag.value ?: return
        val currentSelectionFlag = currentSelection.first
        val unselectedIds = currentSelection.second.map { it.id }.toSet() - selected

        selectionAccountsForFlag.update { null }
        viewModelScope.launch(coroutineDispatcher) {
            repository.saveSelectedAccountsForFlag(currentSelectionFlag.id, selected)
            repository.saveSelectedAccountsForFlag(DEFAULT_FLAG_ID, unselectedIds)
            contentResolver.notifyChange(ACCOUNT_FLAGS_URI, null, false)
         }
    }

    fun onSortOrderConfirmed(sortedIds: LongArray) {
        repository.saveAccountFlagOrder(sortedIds)
    }
}