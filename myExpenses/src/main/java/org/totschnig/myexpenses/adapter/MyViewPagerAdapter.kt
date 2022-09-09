package org.totschnig.myexpenses.adapter

import android.database.Cursor
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
import java.io.Serializable

class MyViewPagerAdapter : ListAdapter<Account, TransactionListViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        return TransactionListViewHolder(
            ComposeView(parent.context).apply {
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
            holder.composeView.setContent {
                Text(text = it.label)
            }
        }
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

class TransactionListViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

data class Account(
    override val id: Long,
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

) : IAccount,
    Serializable {
    override fun toString(): String {
        return label
    }
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