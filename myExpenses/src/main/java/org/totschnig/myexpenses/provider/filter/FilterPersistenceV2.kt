package org.totschnig.myexpenses.provider.filter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Scope must be passed in, if value is read as flow
 */
class FilterPersistenceV2(
    val dataStore: DataStore<Preferences>,
    val prefKey: String,
    scope: CoroutineScope? = null,
) {

    private val preferenceKey = stringPreferencesKey(prefKey)

    private val data = dataStore.data.map {
        it[preferenceKey]?.let { Json.decodeFromString<Criterion?>(it) }
    }

    val whereFilter: StateFlow<Criterion?> by lazy {
        data.stateIn(scope!!, SharingStarted.Lazily, null)
    }

    suspend fun getValue() = data.first()

    suspend fun persist(value: Criterion?) {
        dataStore.edit {
            if (value != null) {
                it[preferenceKey] = Json.encodeToString(value)
            } else {
                it.remove(preferenceKey)
            }
        }
    }

   suspend fun addCriterion(criterion: SimpleCriterion<*>) {
       val oldValue = whereFilter.value
       persist(when (oldValue) {
           null -> criterion
           is AndCriterion -> AndCriterion(oldValue.criteria + criterion)
           is OrCriterion -> OrCriterion(oldValue.criteria + criterion)
           else -> AndCriterion(setOf(oldValue, criterion))
       })
    }
}