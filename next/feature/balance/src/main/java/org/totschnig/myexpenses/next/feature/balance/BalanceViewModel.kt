package org.totschnig.myexpenses.next.feature.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.common.stateWhileShown
import org.totschnig.myexpenses.next.data.balance.BalanceOverview
import org.totschnig.myexpenses.next.data.balance.BalanceRepository
import org.totschnig.myexpenses.next.data.balance.MonthlyBalance

/**
 * The monthly balance of [repository] for the screens, see [MonthlyBalance]
 */
class BalanceViewModel(repository: BalanceRepository) : ViewModel() {

    val balance: StateFlow<AnalysisState<BalanceOverview>> =
        repository.balance.stateWhileShown(viewModelScope, AnalysisState.Loading)
}
