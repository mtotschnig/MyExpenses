package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

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
    fun loadDebts() = showAll().flatMapLatest {
        loadDebts(null, showSealed = it, showZero = it)
    }
}