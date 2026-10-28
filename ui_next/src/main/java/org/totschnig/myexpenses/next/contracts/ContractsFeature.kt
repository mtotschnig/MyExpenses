package org.totschnig.myexpenses.next.contracts

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.totschnig.myexpenses.next.BehindStatusBar
import org.totschnig.myexpenses.next.NextData
import org.totschnig.myexpenses.next.NextFeature
import org.totschnig.myexpenses.next.NextKey
import org.totschnig.myexpenses.next.NextShell
import org.totschnig.myexpenses.next.NextTab
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.feature.contracts.ContractsFlow
import org.totschnig.myexpenses.next.feature.contracts.ContractsViewModel
import org.totschnig.myexpenses.next.feature.contracts.PaymentContractChip
import org.totschnig.myexpenses.next.feature.contracts.PaymentContractSection
import org.totschnig.myexpenses.next.viewModelsOf

/** Contracts and incomes detected in the ledger */
class ContractsFeature(activity: ComponentActivity, data: NextData) : NextFeature {
    val viewModel by activity.viewModelsOf { ContractsViewModel(data.contracts, data.homeCurrency) }

    @Composable
    override fun TransactionSections(transaction: ShownTransaction) {
        PaymentContractSection(viewModel, transaction)
    }

    @Composable
    override fun TransactionChips(transaction: ShownTransaction) {
        PaymentContractChip(viewModel, transaction)
    }

    override val tab = NextTab(Icons.Default.Description, R.string.next_tab_contracts, NextKey.Contracts())

    /** Unread news about contracts; those about incomes are shown in the balance */
    @Composable
    override fun hasNews() = viewModel.news.collectAsStateWithLifecycle().value.any { !it.isRead && !it.isIncome }

    override fun EntryProviderScope<NavKey>.pages(shell: NextShell) {
        entry<NextKey.Contracts> { key ->
            BehindStatusBar(MaterialTheme.colorScheme.surface) {
                ContractsFlow(viewModel, shell.fixedTransactions, openContract = key.openContract)
            }
        }
    }
}
