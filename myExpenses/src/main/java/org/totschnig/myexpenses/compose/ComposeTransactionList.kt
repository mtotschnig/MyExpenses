package org.totschnig.myexpenses.compose

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.ExperimentalFoundationApi
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
import kotlinx.coroutines.flow.StateFlow
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.Transaction2

class ComposeTransactionList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
    lateinit var pagingSourceFactory: () -> PagingSource<Int, Transaction2>
    lateinit var headerData: StateFlow<Map<Int, HeaderData>>

    @OptIn(ExperimentalFoundationApi::class)
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
                val itemCount = lazyPagingItems.itemCount
                var lastHeader: Int? = null

                for (index in 0 until itemCount) {
                    // Gets item without notifying Paging of the item access,
                    // which would otherwise trigger page loads
                    val transaction = lazyPagingItems.peek(index)
                    val headerId = transaction?.let { HeaderData.calculateGroupId(it) }

                    if (transaction !== null && headerId  != lastHeader) {
                        stickyHeader(key = headerId) {
                            Text(text = headerId.toString())
                        }
                    }

                    item(key = transaction?.id) {
                        // Gets item, triggering page loads if needed
                        lazyPagingItems[index]?.let {
                            TransactionRenderer(it)
                        }
                    }

                    lastHeader = headerId
                }
            }
        }
    }

    @Composable
    fun TransactionRenderer(transaction: Transaction2) {
        Row(modifier = Modifier.sizeIn(minHeight = 48.dp)) {
            Text(text = LocalDateFormatter.current.format(transaction.date))
            Text(modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f), text = transaction.label ?: "LABEL")
            ColoredAmountText(money = transaction.amount)
        }
    }
}