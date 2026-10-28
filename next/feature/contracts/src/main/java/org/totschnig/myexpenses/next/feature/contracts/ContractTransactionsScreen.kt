package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import org.totschnig.myexpenses.designsystem.ActionChipRow
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactions
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactionsScreen
import org.totschnig.myexpenses.next.data.contracts.Contract

/**
 * The debits a contract was detected from, shown like the transactions of an account.
 * The list is fixed to the contract: it can neither be searched nor filtered.
 *
 * @param transactionList renders the list, see [ContractTransactionList]
 */
@Composable
fun ContractTransactionsScreen(
    contract: Contract,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    transactionList: @Composable (Modifier) -> Unit,
) {
    FixedTransactionsScreen(
        title = contract.title,
        subtitle = pluralStringResource(
            R.plurals.next_contracts_based_on,
            contract.transactions.size,
            contract.transactions.size
        ),
        onBack = onBack,
        modifier = modifier,
        transactionList = transactionList
    )
}

/**
 * The debits of [contract], updated when further debits are detected
 *
 * @param fixedTransactions shows the debits
 */
@Composable
fun ContractTransactionList(
    viewModel: ContractsViewModel,
    fixedTransactions: FixedTransactions,
    contract: Contract,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(contract.transactions) {
        fixedTransactions.show(contract.displayName, contract.transactions.map { it.id })
    }
    fixedTransactions.List(modifier) {
        PaymentContractSection(viewModel, it)
        ActionChipRow { PaymentContractChip(viewModel, it) }
    }
}
