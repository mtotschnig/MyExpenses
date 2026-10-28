package org.totschnig.myexpenses.next.data.contracts

import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.model.ledger.LedgerSource
import org.totschnig.myexpenses.next.core.common.ledger.Memo
import org.totschnig.myexpenses.next.core.model.ledger.analysisState
import org.totschnig.myexpenses.next.core.common.ledger.mapMemo
import org.totschnig.myexpenses.next.core.model.ledger.valueOrNull
import org.totschnig.myexpenses.next.core.common.logTimings
import org.totschnig.myexpenses.next.core.common.measured
import org.totschnig.myexpenses.next.core.common.shareWhileShown

/**
 * Detects contracts in the debits of all accounts that are not excluded from totals, once for
 * the process, so that screens, widgets and notifications share one analysis. Amounts are
 * converted into the home currency, so that contracts of all accounts can be summed up.
 *
 * Transactions are only analysed after the user agreed. Decisions of the user (dismissed
 * contracts, custom names, categories) are kept in [store], which is part of the backup. The
 * flows run in [scope] while they are collected.
 */
class ContractRepository(
    val ledgerSource: LedgerSource,
    val store: ContractStore,
    private val scope: CoroutineScope,
) {
    private val settings: Flow<ContractSettings> = store.settings

    /** Transactions to analyse, null while loading or without consent */
    private val transactions: Flow<List<ContractTransaction>?> =
        ledgerSource.ledger.mapMemo(Memo("contracts: transactions", ::contractTransactions))

    /**
     * Everything the analysis depends on. Tags, categories and receipts are not part of it, so
     * changing them does not analyse again.
     */
    private data class AnalysisInput(
        val transactions: List<ContractTransaction>,
        val rules: List<ContractRule>,
        val today: LocalDate,
    )

    private val analysisMemo = Memo<AnalysisInput, ContractAnalysis>("contracts: analysis") { (transactions, rules, today) ->
        ContractAnalysis.of(transactions, rules, today)
    }

    /** The analysis the automatic decisions were last made for */
    @Volatile
    private var decidedAnalysis: ContractAnalysis? = null

    private val _latestAnalysis = MutableStateFlow<ContractAnalysis?>(null)

    /** The analysis actions work on, e.g. to create the rule of a suggestion */
    val latestAnalysis: StateFlow<ContractAnalysis?> = _latestAnalysis.asStateFlow()

    /**
     * Confirmed contracts from the rules of the user and suggestions for the other payments,
     * null while loading or without consent
     */
    val analysis: SharedFlow<ContractAnalysis?> =
        combine(transactions, settings.map { it.rules }.distinctUntilChanged(), ledgerSource.today) { transactions, rules, today ->
            transactions?.let { AnalysisInput(it, rules, today) }
        }
            .mapMemo(analysisMemo)
            .onEach {
                _latestAnalysis.value = it
                // A reused analysis was decided on already
                if (it != null && it !== decidedAnalysis) {
                    measured("contracts: automatic decisions") { applyAutomaticDecisions(it) }
                    decidedAnalysis = it
                }
            }
            .logTimings("contracts: analysis")
            .flowOn(Dispatchers.Default)
            // Contracts, incomes and the facts for other features all need the analysis, it should run only once
            .shareWhileShown(scope)

    /**
     * Stores what [automaticDecisions] decides after each analysis. Changes of the rules lead to a
     * new analysis.
     */
    private suspend fun applyAutomaticDecisions(analysis: ContractAnalysis) {
        store.edit { editor ->
            val settings = editor.settings
            val storedNews = editor.news
            val decisions = automaticDecisions(settings, storedNews, analysis, analysis.today)
            if (decisions.marksMigration) {
                decisions.migratedSalary?.let(editor::setSalary)
                editor.markRulesMigrated(dropOld = decisions.migratedSalary != null)
            }
            if (decisions.rules != settings.rules) editor.rules = decisions.rules
            if (decisions.news != storedNews) editor.news = decisions.news
        }
    }

    /** News about confirmed contracts, newest first, see [ContractNews] */
    val news: Flow<List<ContractNews>> = store.news

    private fun overviewOf(direction: ContractDirection) =
        combine(ledgerSource.consent, settings, analysis) { consent, settings, analysis ->
            analysisState(consent, analysis?.let {
                buildContractsState(
                    it.contracts.filter { contract -> contract.direction == direction },
                    settings,
                    it.dismissed.filter { contract -> contract.direction == direction }
                )
            })
        }.shareWhileShown(scope)

    /** Contracts, i.e. recurring debits */
    val expenses: SharedFlow<AnalysisState<ContractsOverview>> = overviewOf(ContractDirection.EXPENSE)

    /** Recurring credits, e.g. salary */
    val incomes: SharedFlow<AnalysisState<ContractsOverview>> = overviewOf(ContractDirection.INCOME)

    /** What other features know about contracts, null while loading or without consent */
    val facts: SharedFlow<ContractFacts?> =
        combine(expenses, incomes, analysis) { expenseState, incomeState, analysis ->
            val expenses = expenseState.valueOrNull
            val incomes = incomeState.valueOrNull
            if (expenses != null && incomes != null && analysis != null) {
                ContractFacts(
                    active = expenses.active,
                    // Payments of cancelled contracts, e.g. the last one, are still contract payments
                    confirmed = expenses.active + expenses.ended + expenses.cancelled,
                    salaries = incomes.salaries,
                    paymentIds = analysis.contracts.flatMapTo(HashSet()) { contract -> contract.transactions.map { it.id } },
                    transactions = analysis.transactions
                )
            } else null
        }
            .distinctUntilChanged()
            .logTimings("contracts: facts")
            .shareWhileShown(scope)
}
