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
import org.totschnig.myexpenses.util.enumValueOrDefault

/**
 * A generic interface to map a custom type U (like an enum) to a
 * persistable type P (like String or Int).
 */
interface Mapper<U, P> {
    fun toPreference(userValue: U): P
    fun fromPreference(persistedValue: P): U

    /**
     * A canonical identity mapper that can be reused for any type
     * that does not require conversion.
     */
    object Identity : Mapper<Any, Any> {
        override fun toPreference(userValue: Any): Any = userValue
        override fun fromPreference(persistedValue: Any): Any = persistedValue
    }
}

/**
 * A helper class that encapsulates the logic for reading and writing
 * a specific preference from a DataStore.
 *
 * It can handle direct persistence of primitive types (T) or be configured
 * with a mapper to handle custom types (U) that are persisted as a primitive (P).
 *
 * @param dataStore The DataStore instance.
 * @param key The key for the *persisted* preference type P.
 * @param defaultValue The default value for the *persisted* type P.
 * @param mapper An optional mapper to convert between a user-facing type U
 * and a persisted type P. Defaults to an identity mapper.
 */
class PreferenceAccessor<U, P>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<P>,
    private val defaultValue: U,
    private val mapper: Mapper<U, P>,
) {
    /**
     * A Flow that emits the current value of the preference, converted to the
     * user-facing type U.
     */
    val flow: Flow<U> = dataStore.data.map { preferences ->
        preferences[key]?.let { mapper.fromPreference(it) } ?: defaultValue
    }

    /**
     * Persists a new user-facing value by converting it to the persisted type
     * and writing it to the DataStore.
     */
    suspend fun set(value: U) {
        val forPreference = mapper.toPreference(value)
        dataStore.edit { preferences ->
            preferences[key] = forPreference
        }
    }

    @Composable
    fun asState(): MutableState<U> {
        val scope = rememberCoroutineScope()
        val state = flow.collectAsState(initial = defaultValue)

        return remember(this) {
            object : MutableState<U> {
                override var value: U
                    get() = state.value
                    set(new) {
                        scope.launch { set(new) }
                    }

                override fun component1() = value
                override fun component2(): (U) -> Unit = { value = it }
            }
        }
    }
}

/**
 * A convenience factory function to create a PreferenceAccessor for primitive types
 * where the user-facing type and the persisted type are the same.
 */
fun <T> PreferenceAccessor(
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<T>,
    defaultValue: T,
): PreferenceAccessor<T, T> {
    @Suppress("UNCHECKED_CAST")
    return PreferenceAccessor(dataStore, key, defaultValue, Mapper.Identity as Mapper<T, T>)
}

/**
 * A convenience factory function to create a PreferenceAccessor for enums,
 * which are persisted as Strings.
 */
@Suppress("FunctionName")
inline fun <reified E : Enum<E>> EnumPreferenceAccessor(
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<String>,
    defaultValue: E,
): PreferenceAccessor<E, String> {
    val enumMapper = object : Mapper<E, String> {
        override fun toPreference(userValue: E): String = userValue.name
        override fun fromPreference(persistedValue: String) =
            enumValueOrDefault(persistedValue, defaultValue)
    }
    return PreferenceAccessor(dataStore, key, defaultValue, enumMapper)
}
