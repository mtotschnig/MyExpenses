package org.totschnig.myexpenses.next.core.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.totschnig.myexpenses.designsystem.PageSubtitle
import org.totschnig.myexpenses.designsystem.PageTopBar
import org.totschnig.myexpenses.next.core.model.transactions.ShownTransaction

/**
 * A fixed set of transactions, e.g. the debits of a contract or the payments of an income,
 * shown like the transaction list of an account. The app provides it, so that features show
 * transactions without knowing how they are loaded.
 */
interface FixedTransactions {
    /** Sets the transactions [List] shows, under [label] */
    fun show(label: String, ids: List<Long>)

    /**
     * The transactions set with [show]. A tap opens their details, which can be changed there if
     * the app allows it.
     *
     * @param detailsContent added to the details of a transaction
     */
    @Composable
    fun List(modifier: Modifier, detailsContent: (@Composable (ShownTransaction) -> Unit)?)
}

/**
 * A fixed set of transactions with a title, e.g. the debits of a contract
 */
@Composable
fun FixedTransactionsScreen(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    transactionList: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        PageTopBar(
            onBack = onBack,
            title = title,
            subtitle = subtitle?.let { { PageSubtitle(it) } }
        )
        transactionList(Modifier.weight(1f))
    }
}
