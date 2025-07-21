package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.enumValueOrDefault

class HistoryViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {
    private val accountId by lazy { savedStateHandle.get<Long>(DatabaseConstants.KEY_ACCOUNTID) }
    private val defaultGrouping: Grouping by lazy {
        savedStateHandle.get<Grouping>(DatabaseConstants.KEY_GROUPING)
            .takeIf { it != Grouping.NONE }
            ?: Grouping.MONTH
    }
    private val groupingPrefKey = stringPreferencesKey("historyGrouping_$accountId")

    val grouping by lazy {
        dataStore.data.map {
            enumValueOrDefault(it[groupingPrefKey], defaultGrouping)
        }.stateIn(viewModelScope, SharingStarted.Lazily, defaultGrouping)
    }

    fun accountInfo(accountId: Long) = combine(account(accountId), grouping) {
        account, grouping -> account to grouping
    }

    fun persistGrouping(grouping: Grouping) {
        viewModelScope.launch {
            dataStore.edit { preference ->
                preference[groupingPrefKey] = grouping.name
            }
        }
    }
}