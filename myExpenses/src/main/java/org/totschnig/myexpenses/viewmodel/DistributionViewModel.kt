package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.util.enumValueOrDefault

class DistributionViewModel(application: Application, savedStateHandle: SavedStateHandle):
    DistributionViewModelBase<Account>(application, savedStateHandle) {
    private fun getGroupingPrefKey(accountId: Long) = stringPreferencesKey("distributionGrouping_$accountId")
    fun initWithAccount(accountId: Long, defaultGrouping: Grouping) {
        viewModelScope.launch {
            account(accountId, true).asFlow().collect {
                _accountInfo.tryEmit(it)
            }
        }
        viewModelScope.launch {
            dataStore.data.map {
                enumValueOrDefault(it[getGroupingPrefKey(accountId)], defaultGrouping)
            }.collect {
                setGrouping(it)
            }
        }
    }

    fun persistGrouping(grouping: Grouping) {
        accountInfo.value?.let {
            viewModelScope.launch {
                dataStore.edit { preference ->
                    preference[getGroupingPrefKey(it.id)] = grouping.name
                }
            }
        }
    }
}
