package org.totschnig.myexpenses.next.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.totschnig.myexpenses.compose.transactions.FutureCriterion
import org.totschnig.myexpenses.compose.transactions.TransactionEvent
import org.totschnig.myexpenses.compose.transactions.TransactionEventHandler
import org.totschnig.myexpenses.compose.transactions.TransactionListContent
import org.totschnig.myexpenses.next.NextTransactionList
import org.totschnig.myexpenses.viewmodel.data.HeaderDataEmpty
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction
import org.totschnig.myexpenses.next.core.ui.transactions.FixedTransactions

/**
 * What can be done with a transaction from its details, like in the transaction list of an account
 *
 * @param accountCount number of accounts, transforming into a transfer needs at least two
 */
class TransactionActions(val onEvent: TransactionEventHandler, val accountCount: Int)

private val NoTransactionEvents = object : TransactionEventHandler {
    override fun invoke(event: TransactionEvent, transaction: Transaction2) {}
}

/**
 * Read-only transaction list of the new design. Without day headers, since balances make no sense
 * across accounts, so each transaction shows its date and account.
 */
@Composable
internal fun FixedTransactionList(
    items: LazyPagingItems<Transaction2>,
    modifier: Modifier = Modifier,
    /** Offered in the details of a transaction, null if transactions cannot be changed */
    actions: TransactionActions? = null,
    /** Added to the details of a transaction, which a tap opens */
    detailsContent: (@Composable (Transaction2) -> Unit)? = null,
) {
    val content = remember(items, actions) {
        TransactionListContent(
            lazyPagingItems = items,
            headerData = HeaderDataEmpty,
            selectionHandler = null,
            onEvent = actions?.onEvent ?: NoTransactionEvents,
            modificationAllowed = actions != null,
            accountCount = actions?.accountCount ?: 0,
            isFiltered = true,
            futureCriterion = FutureCriterion.EndOfDay
        )
    }
    NextTransactionList(
        content = content,
        modifier = modifier,
        isReadOnly = true,
        showDateAndAccount = true,
        detailsContent = detailsContent
    )
}

/**
 * The [FixedTransactions] the features show: those of [viewModel] in the list of the new design,
 * with [actions] in their details
 */
class AppFixedTransactions(
    private val viewModel: FixedTransactionsViewModel,
    private val actions: TransactionActions?,
) : FixedTransactions {
    override fun show(label: String, ids: List<Long>) = viewModel.show(label, ids)

    @Composable
    override fun List(modifier: Modifier, detailsContent: (@Composable (ShownTransaction) -> Unit)?) {
        FixedTransactionList(
            viewModel.transactions.collectAsLazyPagingItems(),
            modifier,
            actions,
            detailsContent?.let { content -> @Composable { transaction: Transaction2 -> content(transaction.toShown()) } }
        )
    }
}
