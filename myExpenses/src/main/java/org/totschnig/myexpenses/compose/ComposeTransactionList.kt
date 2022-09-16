package org.totschnig.myexpenses.compose

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import org.totschnig.myexpenses.viewmodel.data.Transaction

class ComposeTransactionList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
    lateinit var pagingSourceFactory: () -> PagingSource<Int, Transaction>

    @Composable
    override fun Content() {
        AppTheme(context = context) {
            val pager = remember {
                Pager(
                    PagingConfig(
                        pageSize = 100,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = pagingSourceFactory
                )
            }
            val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
            LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
                itemsIndexed(lazyPagingItems) { index, transaction ->
                    if (transaction != null)
                        TransactionRenderer(transaction)
                }
            }
        }
    }

    @Composable
    fun TransactionRenderer(transaction: Transaction) {
        Row(modifier = Modifier.sizeIn(minHeight = 48.dp)) {
            Text(text = LocalDateFormatter.current.format(transaction.date))
            Text(modifier = Modifier.padding(horizontal = 5.dp).weight(1f), text = transaction.label ?: "LABEL")
            ColoredAmountText(money = transaction.amount)
        }
    }
}