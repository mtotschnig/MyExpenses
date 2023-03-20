package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.viewmodel.data.Debt

class DebtOverViewViewModel(application: Application) : DebtViewModel(application) {
    private val showAllPrefKey = booleanPreferencesKey("showAll")

    fun showAll() =
        dataStore.data.map { preferences ->
            preferences[showAllPrefKey] ?: false
        }

    suspend fun persistShowAll(showAll: Boolean) {
        dataStore.edit { preference ->
            preference[showAllPrefKey] = showAll
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val debts: StateFlow<List<Debt>>
        get() = showAll().flatMapLatest {
            loadDebts(null, showSealed = it, showZero = it)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}