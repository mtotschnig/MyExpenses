package org.totschnig.myexpenses.next.balance

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.totschnig.myexpenses.designsystem.ActionChipRow
import org.totschnig.myexpenses.next.BehindStatusBar
import org.totschnig.myexpenses.next.NextData
import org.totschnig.myexpenses.next.NextFeature
import org.totschnig.myexpenses.next.NextKey
import org.totschnig.myexpenses.next.NextNavigation
import org.totschnig.myexpenses.next.NextShell
import org.totschnig.myexpenses.next.contracts.ContractsFeature
import org.totschnig.myexpenses.next.feature.balance.BalanceIncomes
import org.totschnig.myexpenses.next.feature.balance.BalanceViewModel
import org.totschnig.myexpenses.next.feature.balance.MonthlyBalanceCard
import org.totschnig.myexpenses.next.feature.balance.MonthlyBalanceFlow
import org.totschnig.myexpenses.next.feature.contracts.IncomeFlow
import org.totschnig.myexpenses.next.feature.contracts.SalaryChoiceSheet
import org.totschnig.myexpenses.next.viewModelsOf

/**
 * The balance of the month: what comes in, what is fixed and what is left
 *
 * @param contracts the incomes and the fixed costs the balance is made of
 */
class BalanceFeature(
    activity: ComponentActivity,
    private val data: NextData,
    private val contracts: ContractsFeature,
) : NextFeature {
    val viewModel by activity.viewModelsOf { BalanceViewModel(data.balance) }

    @Composable
    override fun OverviewCard(navigation: NextNavigation) {
        val balance by viewModel.balance.collectAsStateWithLifecycle()
        val homeCurrency by data.homeCurrency.collectAsStateWithLifecycle()
        MonthlyBalanceCard(
            state = balance,
            currency = homeCurrency,
            onOpen = { navigation.show(NextKey.Balance) },
            onConsent = { data.ledgerSource.setConsent(true) },
            modifier = Modifier
        )
    }

    override fun EntryProviderScope<NavKey>.pages(shell: NextShell) {
        entry<NextKey.Balance> {
            BehindStatusBar(MaterialTheme.colorScheme.surfaceContainer) {
                val homeCurrency by data.homeCurrency.collectAsStateWithLifecycle()
                val contractsViewModel = contracts.viewModel
                MonthlyBalanceFlow(
                    viewModel,
                    BalanceIncomes(
                        state = contractsViewModel.incomeState,
                        news = contractsViewModel.news,
                        page = { onBack -> IncomeFlow(contractsViewModel, shell.fixedTransactions, onBack) },
                        salarySheet = { onDismiss -> SalaryChoiceSheet(contractsViewModel, onDismiss) }
                    ),
                    homeCurrency, shell.fixedTransactions,
                    onBack = { shell.navigation.close(NextKey.Balance) },
                    onOpenContracts = { shell.navigation.show(NextKey.Contracts()) }
                ) { transaction ->
                    contracts.TransactionSections(transaction)
                    ActionChipRow { contracts.TransactionChips(transaction) }
                }
            }
        }
    }
}
