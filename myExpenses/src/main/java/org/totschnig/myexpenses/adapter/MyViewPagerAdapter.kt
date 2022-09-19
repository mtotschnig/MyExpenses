package org.totschnig.myexpenses.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.paging.PagingSource
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.compose.ComposeTransactionList
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.viewmodel.data.Transaction2

class MyViewPagerAdapter(val loader: (FullAccount) ->  Pair<() -> PagingSource<Int, Transaction2>, Flow<HeaderData>>) :
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
        with(holder.composeView) {
            val account = getItem(position)
            with(loader(account)) {
                pagingSourceFactory = first
                headerData = second
                accountId = account.id
            }
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
                return oldItem == newItem
            }
        }
    }
}

class TransactionListViewHolder(val composeView: ComposeTransactionList) :
    RecyclerView.ViewHolder(composeView)