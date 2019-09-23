package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import androidx.recyclerview.widget.DiffUtil
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*



data class Budget(val id: Long, val accountId: Long, val title: String, val description: String,
                  val currency: String, val amount: Money, val grouping: Grouping) {

    fun toContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE, title)
        contentValues.put(KEY_DESCRIPTION, description)
        contentValues.put(KEY_GROUPING, grouping.name)
        contentValues.put(KEY_BUDGET, amount.amountMinor)
        if (accountId > 0) {
            contentValues.put(KEY_ACCOUNTID, accountId)
        } else {
            contentValues.put(KEY_CURRENCY, currency)
        }
        return contentValues
    }


    companion object {
        val BUDGET_TYPES = arrayOf(Grouping.YEAR, Grouping.MONTH, Grouping.WEEK, Grouping.DAY)
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Budget>() {
            override fun areItemsTheSame(oldItem: Budget, newItem: Budget): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Budget, newItem: Budget): Boolean {
                return oldItem == newItem
            }

        }
    }
}
