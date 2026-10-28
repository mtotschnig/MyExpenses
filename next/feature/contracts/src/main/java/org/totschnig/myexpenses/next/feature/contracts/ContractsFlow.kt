package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactions
import org.totschnig.myexpenses.next.data.contracts.contractTransactions

/**
 * The tab "Verträge": the contracts with their details, news and debits
 *
 * @param openContract signature of a contract shown from the start
 */
@Composable
fun ContractsFlow(
    viewModel: ContractsViewModel,
    fixedTransactions: FixedTransactions,
    modifier: Modifier = Modifier,
    openContract: String? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val news by viewModel.news.collectAsStateWithLifecycle()
    NextContractsScreen(
        state = state,
        currency = viewModel.homeCurrency.collectAsStateWithLifecycle().value,
        modifier = modifier,
        actions = viewModel,
        onConsent = viewModel.ledgerSource::setConsent,
        news = news,
        openContract = openContract,
        contractTransactions = { contract, contentModifier ->
            ContractTransactionList(
                viewModel,
                fixedTransactions,
                contract,
                contentModifier
            )
        }
    )
}
