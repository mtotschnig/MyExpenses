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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.export.pdf.DebtOverviewPdfGenerator
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.util.AppDirHelper
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

    fun print() {
        viewModelScope.launch(coroutineContext()) {
            _pdfResult.update {
                AppDirHelper.checkAppDir(getApplication()).mapCatching { destDir ->
                    DebtOverviewPdfGenerator(localizedContext).generatePdf(
                        destDir = destDir,
                        groups = debts.value.second.groupBy { it.payeeId }.values
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val debts: StateFlow<Pair<Sort, List<DisplayDebt>>> by lazy {
        combine(showAll(), sortOrder(), sortDirection()) { showAll, sortOrder, sortDirection ->
            Triple(showAll, sortOrder, sortDirection)
        }.flatMapLatest { (showAll, sortOrder, sortDirection) ->
            loadDebts(
                null,
                showSealed = showAll,
                showZero = showAll,
                sortOrder = sortOrder.toOrderBy(collate, sortDirection == SortDirection.DESC)
            ).map { debts ->
                sortOrder to if (sortOrder == Sort.DEBT_SUM) {
                    when (sortDirection) {
                        SortDirection.ASC -> debts.sortedBy { it.currentEquivalentBalance }
                        SortDirection.DESC -> debts.sortedByDescending { it.currentEquivalentBalance }
                    }
                } else debts
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, Sort.LABEL to emptyList())
    }
}