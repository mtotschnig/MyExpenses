package org.totschnig.myexpenses.next.data.balance

import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractFacts
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.model.ledger.Ledger
import org.totschnig.myexpenses.next.core.model.ledger.LedgerSource
import org.totschnig.myexpenses.next.core.common.ledger.Memo
import org.totschnig.myexpenses.next.core.model.ledger.analysisState
import org.totschnig.myexpenses.next.core.common.logTimings
import org.totschnig.myexpenses.next.core.common.shareWhileShown

/**
 * The monthly balance, see [MonthlyBalance], once for the process, for the overview, widgets and
 * notifications. The flows run in [scope] while they are collected.
 */
class BalanceRepository(
    ledgerSource: LedgerSource,
    contractFacts: Flow<ContractFacts?>,
    scope: CoroutineScope,
) {
    /**
     * What came in and went out of the daily accounts since the last salary, including the
     * contract debits still to come until the next one
     */
    data class BalanceInput(
        val ledger: Ledger,
        val confirmed: List<Contract>,
        val salaries: List<Contract>,
        val today: LocalDate,
    )

    val memo = Memo<BalanceInput, BalanceOverview>("balance: month") { (ledger, confirmed, salaries, today) ->
        val period = BalancePeriod.of(salaries.firstOrNull(), today)
        BalanceOverview(
            MonthlyBalance.compute(period, balanceTransactions(ledger, period), ledger.ownAccounts.dailyIds, confirmed, salaries),
            today
        )
    }

    val balance: SharedFlow<AnalysisState<BalanceOverview>> =
        combine(ledgerSource.consent, ledgerSource.ledger, contractFacts, ledgerSource.today) { consent, ledger, facts, today ->
            analysisState(consent, if (ledger != null && facts != null) {
                memo.of(BalanceInput(ledger, facts.confirmed, facts.salaries, today))
            } else null)
        }
            .logTimings("balance: month")
            .flowOn(Dispatchers.Default)
            .shareWhileShown(scope)
}
