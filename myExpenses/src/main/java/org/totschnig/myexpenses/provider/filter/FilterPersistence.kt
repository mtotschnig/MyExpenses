package org.totschnig.myexpenses.provider.filter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import kotlin.collections.first

/**
 * Scope must be passed in, if value is read as flow
 */
class FilterPersistence(
    val dataStore: DataStore<Preferences>,
    val prefKey: String,
    scope: CoroutineScope? = null,
) {

    private val preferenceKey = stringPreferencesKey(prefKey)

    private val mapper: (Preferences) -> Criterion? = {
        it[preferenceKey]?.let { Json.decodeFromString<Criterion?>(it) }
    }
    private val data = dataStore.data.map(mapper)


    val whereFilter: StateFlow<Criterion?> by lazy {
        data.stateIn(scope!!, SharingStarted.Lazily, null)
    }

    suspend fun getValue() = data.first()

    suspend fun persist(list: Collection<Criterion>) {
        persist(when(list.size) {
            0 -> null
            1 -> list.first()
            else -> AndCriterion(list.toSet())
        })
    }

    private fun MutablePreferences.persist(value: Criterion?) {
        if (value != null) {
            this[preferenceKey] = Json.encodeToString(value)
        } else {
            remove(preferenceKey)
        }
    }

    suspend fun update(transform: (Criterion?) -> Criterion) {
        dataStore.edit { it.persist(transform(mapper(it))) }
    }

    suspend fun persist(value: Criterion?) {
        dataStore.edit { it.persist(value) }
    }

   suspend fun addCriterion(criterion: SimpleCriterion<*>) {
      update { oldValue ->  when (oldValue) {
          null -> criterion
          is AndCriterion -> AndCriterion(oldValue.criteria + criterion)
          is OrCriterion -> OrCriterion(oldValue.criteria + criterion)
          else -> AndCriterion(setOf(oldValue, criterion))
      }}
    }

    suspend fun replaceCriterion(criterion: SimpleCriterion<*>) {
        update { oldValue ->  when (oldValue) {
            null -> throw IllegalStateException()
            is AndCriterion -> AndCriterion(oldValue.criteria.map {
                if(it::class == criterion::class) criterion else it
            }.toSet())
            is OrCriterion -> OrCriterion(oldValue.criteria.map {
                if(it::class == criterion::class) criterion else it
            }.toSet())
            else -> criterion
        }}
    }
}