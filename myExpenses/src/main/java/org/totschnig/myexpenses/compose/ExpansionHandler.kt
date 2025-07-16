package org.totschnig.myexpenses.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.util.toggle

interface ExpansionHandler {
    val state: Flow<Set<String>?>
    fun toggle(id: String)
}

class DataStoreExpansionHandler(
    key: String,
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) : ExpansionHandler {
    val prefKey = stringSetPreferencesKey(key)
    override val state: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[prefKey] ?: emptySet()
    }

    override fun toggle(id: String) {
        scope.launch {
            dataStore.edit { settings ->
                settings[prefKey] =
                    settings[prefKey]?.toMutableSet()?.also {
                        it.toggle(id)
                    } ?: setOf(id)
            }
        }
    }
}