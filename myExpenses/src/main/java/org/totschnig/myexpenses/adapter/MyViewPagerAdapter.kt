package org.totschnig.myexpenses.adapter

import android.content.Context
import android.database.Cursor
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.AbstractComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.getDouble
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrDefault

class MyViewPagerAdapter(val loader: (Long) -> Flow<List<Transaction>>) : ListAdapter<Account, TransactionListViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        return TransactionListViewHolder(
            ComposeTransactionList(parent.context).apply {
                layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
            }
        )
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {
        getItem(position).let {
            holder.composeView.transactionList = loader(getItem(position).id)
        }
    }

    override fun onViewDetachedFromWindow(holder: TransactionListViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.composeView.transactionList.cancellable()
    }

    public override fun getItem(position: Int): Account {
        return super.getItem(position)
    }

    fun setData(data: List<Account>) {
        submitList(data)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Account>() {
            override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class TransactionListViewHolder(val composeView: ComposeTransactionList) : RecyclerView.ViewHolder(composeView)

data class Account(
    val id: Long,
    val label: String,
    val currency: CurrencyUnit,
    val color: Int = -1,
    val type: AccountType = AccountType.CASH,
    val exchangeRate: Double = 1.0,
    val sealed: Boolean,
    val currentBalance: Long,
    val grouping: Grouping,
    val sortDirection: SortDirection,
    val syncAccountName: String?

) {
    companion object {
        fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext) = Account(
            cursor.getLong(KEY_ROWID),
            cursor.getString(KEY_LABEL),
            currencyContext.get(cursor.getString(KEY_CURRENCY)),
            cursor.getInt(KEY_COLOR),
            enumValueOrDefault(cursor.getString(KEY_TYPE), AccountType.CASH),
            cursor.getDouble(KEY_EXCHANGE_RATE),
            cursor.getInt(KEY_SEALED) == 1,
            cursor.getLong(KEY_CURRENT_BALANCE),
            enumValueOrDefault(cursor.getString(KEY_GROUPING), Grouping.NONE),
            enumValueOrDefault(cursor.getString(KEY_SORT_DIRECTION), SortDirection.DESC),
            cursor.getStringOrNull(KEY_SYNC_ACCOUNT_NAME)
        )
    }
}

data class Transaction(
    val id: Long,
    val amount: Long
)

class ComposeTransactionList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
    lateinit var transactionList: Flow<List<Transaction>>

    @Composable
    override fun Content() {
        val list = transactionList.collectAsState(initial = emptyList())
        LazyColumn {
            itemsIndexed(list.value) { _, item ->
                Text(item.amount.toString())
            }
        }
    }
}