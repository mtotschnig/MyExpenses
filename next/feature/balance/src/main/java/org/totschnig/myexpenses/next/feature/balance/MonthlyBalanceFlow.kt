package org.totschnig.myexpenses.next.feature.balance

import org.totschnig.myexpenses.next.data.contracts.salaries
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.data.contracts.ContractNews
import org.totschnig.myexpenses.next.data.contracts.ContractsOverview
import org.totschnig.myexpenses.next.core.model.ledger.AnalysisState
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.model.ledger.valueOrNull
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactionsScreen
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactions
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.data.balance.BalanceOverview

/** The pages behind the balance card, the breakdown at the bottom of the back stack */
@Serializable
sealed interface BalanceKey : NavKey {
    @Serializable
    data object Details : BalanceKey

    /** The regular incomes, with a back stack of their own for their details */
    @Serializable
    data object Income : BalanceKey

    /** @param savings the savings instead of the other expenses */
    @Serializable
    data class Transactions(val savings: Boolean) : BalanceKey
}

/**
 * The pages behind the balance card of the overview: the breakdown, the regular incomes
 * and the transactions of savings and other expenses, on a back stack of their own ([BalanceKey])
 *
 * @param onBack leaves the breakdown with the arrow of its top bar; back itself is handled by the
 * back stack of the shell, on which the breakdown is a page
 * @param onOpenContracts shows the contracts, which have their own tab
 * @param transactionDetails added to the details of the transactions of savings and other
 * expenses, e.g. their contract
 */
@Composable
fun MonthlyBalanceFlow(
    balanceViewModel: BalanceViewModel,
    incomes: BalanceIncomes,
    currency: CurrencyInfo,
    fixedTransactions: FixedTransactions,
    onBack: () -> Unit,
    onOpenContracts: () -> Unit,
    modifier: Modifier = Modifier,
    transactionDetails: @Composable (ShownTransaction) -> Unit = {},
) {
    val backStack = rememberNavBackStack(BalanceKey.Details)
    val ready = balanceViewModel.balance.collectAsStateWithLifecycle().value.valueOrNull
    if (ready == null) {
        CircularProgressIndicator(modifier.fillMaxSize().wrapContentSize())
        return
    }
    val current by rememberUpdatedState(
        BalanceContent(incomes, currency, fixedTransactions, ready, onBack, onOpenContracts, transactionDetails)
    )
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider = entryProvider {
            entry<BalanceKey.Details> { DetailsEntry(current, backStack) }
            entry<BalanceKey.Income> { IncomeEntry(current, backStack) }
            entry<BalanceKey.Transactions> { key -> TransactionsEntry(current, backStack, key) }
        }
    )
}

/**
 * What the balance shows of the regular incomes, which belong to the contracts. The app hands it
 * in, so that the balance does not depend on the screens of the contracts.
 *
 * @param page the regular incomes with their details
 * @param salarySheet chooses the salary the period starts with
 */
class BalanceIncomes(
    val state: StateFlow<AnalysisState<ContractsOverview>>,
    val news: StateFlow<List<ContractNews>>,
    val page: @Composable (onBack: () -> Unit) -> Unit,
    val salarySheet: @Composable (onDismiss: () -> Unit) -> Unit,
)

/** What the pages on the back stack of [MonthlyBalanceFlow] need */
private class BalanceContent(
    val incomes: BalanceIncomes,
    val currency: CurrencyInfo,
    val fixedTransactions: FixedTransactions,
    val ready: BalanceOverview,
    val onBack: () -> Unit,
    val onOpenContracts: () -> Unit,
    val transactionDetails: @Composable (ShownTransaction) -> Unit,
)

@Composable
private fun DetailsEntry(content: BalanceContent, backStack: NavBackStack<NavKey>) {
    val balance = content.ready.balance
    val incomes = content.incomes.state.collectAsStateWithLifecycle().value.valueOrNull
    var showSalarySheet by rememberSaveable { mutableStateOf(false) }
    if (showSalarySheet && incomes != null) {
        content.incomes.salarySheet { showSalarySheet = false }
    }
    MonthlyBalanceScreen(
        balance = balance,
        today = content.ready.today,
        currency = content.currency,
        onBack = content.onBack,
        onOpenIncome = { backStack.add(BalanceKey.Income) },
        onOpenContracts = content.onOpenContracts,
        onOpenSavings = { backStack += BalanceKey.Transactions(savings = true) }
            .takeIf { balance.savingsTransactionIds.isNotEmpty() },
        onOpenOther = { backStack += BalanceKey.Transactions(savings = false) }
            .takeIf { balance.otherTransactionIds.isNotEmpty() },
        hasIncomeNews = content.incomes.news.collectAsStateWithLifecycle().value.any { !it.isRead && it.isIncome },
        salaryName = incomes?.salaries?.takeIf { balance.period.isSalaryCycle && it.isNotEmpty() }
            ?.joinToString { it.displayName },
        onChangeSalary = { showSalarySheet = true }
    )
}

@Composable
private fun IncomeEntry(content: BalanceContent, backStack: NavBackStack<NavKey>) {
    content.incomes.page { backStack.remove(BalanceKey.Income) }
}

@Composable
private fun TransactionsEntry(content: BalanceContent, backStack: NavBackStack<NavKey>, key: BalanceKey.Transactions) {
    val balance = content.ready.balance
    val label = stringResource(if (key.savings) R.string.next_balance_savings else R.string.next_balance_other)
    val ids = if (key.savings) balance.savingsTransactionIds else balance.otherTransactionIds
    LaunchedEffect(ids) { content.fixedTransactions.show(label, ids) }
    FixedTransactionsScreen(
        title = label,
        subtitle = balance.period.label(),
        onBack = { backStack.remove(key) }
    ) {
        content.fixedTransactions.List(it, content.transactionDetails)
    }
}
