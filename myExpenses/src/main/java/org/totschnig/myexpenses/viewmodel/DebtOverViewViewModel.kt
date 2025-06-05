package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt

class DebtOverViewViewModel(application: Application) : DebtViewModel(application) {
    private val showAllPrefKey = booleanPreferencesKey("showAll")
    private val sortOrderPrefKey = stringPreferencesKey("debtOverViewSortOrder")
    private val sortDirectionPrefKey = stringPreferencesKey("debtOverViewSortDirection")

    fun showAll() =
        dataStore.data.map { preferences ->
            preferences[showAllPrefKey] == true
        }

    suspend fun persistShowAll(showAll: Boolean) {
        dataStore.edit { preference ->
            preference[showAllPrefKey] = showAll
        }
    }

    fun sortOrder() =
        dataStore.data.map { preferences ->
            enumValueOrDefault<Sort>(preferences[sortOrderPrefKey], Sort.LABEL).let {
                if (it == Sort.AMOUNT) Sort.DEBT_SUM else it
            }
        }

    suspend fun persistSortOrder(sort: Sort) {
        dataStore.edit { preference ->
            preference[sortOrderPrefKey] = sort.name
        }
    }

    fun sortDirection() =
        dataStore.data.map { preferences ->
            enumValueOrDefault<SortDirection>(preferences[sortDirectionPrefKey], SortDirection.ASC)
        }

    suspend fun persistSortDirection(sortDirection: SortDirection) {
        dataStore.edit { preference ->
            preference[sortDirectionPrefKey] = sortDirection.name
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val debts: StateFlow<Pair<Sort, List<DisplayDebt>>>
        get() = combine(showAll(), sortOrder(), sortDirection()) {
            showAll, sortOrder, sortDirection -> Triple(showAll, sortOrder, sortDirection)
        }.flatMapLatest { (showAll, sortOrder, sortDirection) ->
            loadDebts(
                null,
                showSealed = showAll,
                showZero = showAll,
                sortOrder = sortOrder.toOrderBy(collate, sortDirection == SortDirection.DESC)
            ).map { sortOrder to if (sortOrder == Sort.DEBT_SUM)
                when(sortDirection) {
                    SortDirection.ASC -> it.sortedBy { it.currentEquivalentBalance }
                    SortDirection.DESC -> it.sortedByDescending { it.currentEquivalentBalance }
                }
            else it  }
        }.stateIn(viewModelScope, SharingStarted.Lazily, Sort.LABEL to emptyList())
}