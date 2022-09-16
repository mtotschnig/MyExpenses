package org.totschnig.myexpenses.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.paging.PagingSource
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.compose.ComposeTransactionList
import org.totschnig.myexpenses.viewmodel.data.Transaction
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