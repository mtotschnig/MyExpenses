package org.totschnig.myexpenses.adapter

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.viewmodel.data.FullAccount

class MyViewPagerAdapter(val loader: (Long) -> () -> PagingSource<Int, Transaction>) :
    ListAdapter<FullAccount, TransactionListViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        return TransactionListViewHolder(
            ComposeTransactionList(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            }
        )
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {
        getItem(position).let {
            holder.composeView.pagingSourceFactory = loader(getItem(position).id)
        }
    }

    public override fun getItem(position: Int): FullAccount {
        return super.getItem(position)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullAccount>() {
            override fun areItemsTheSame(oldItem: FullAccount, newItem: FullAccount): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FullAccount, newItem: FullAccount): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}

class TransactionListViewHolder(val composeView: ComposeTransactionList) :
    RecyclerView.ViewHolder(composeView)

data class Transaction(
    val id: Long,
    val amount: Long
)

class ComposeTransactionList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
    lateinit var pagingSourceFactory: () -> PagingSource<Int, Transaction>

    @Composable
    override fun Content() {
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
        LazyColumn {
            items(lazyPagingItems) {
                Text("Item is $it")
            }
        }
    }
}