package org.totschnig.myexpenses.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A helper class that encapsulates the logic for reading and writing
 * a specific preference from a DataStore.
 *
 * @param dataStore The DataStore instance.
 * @param key The key for the preference.
 * @param defaultValue The default value to return if the key is not present.
 */
class PreferenceAccessor<T>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val defaultValue: T
) {
    /**
     * A Flow that emits the current value of the preference whenever it changes.
     * If the key is not set, it emits the [defaultValue].
     */
    val flow: Flow<T> = dataStore.data.map { preferences ->
        preferences[key] ?: defaultValue
    }

    /**
     * Persists a new value for the preference.
     */
    suspend fun set(value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    @Composable
    fun asState(): MutableState<T> {
        val scope = rememberCoroutineScope()
        val state = flow.collectAsState(initial = defaultValue)

        return remember {
            object : MutableState<T> {
                override var value: T
                    get() = state.value
                    set(new) {
                        scope.launch { set(new) }
                    }

                override fun component1() = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
    }
}
    