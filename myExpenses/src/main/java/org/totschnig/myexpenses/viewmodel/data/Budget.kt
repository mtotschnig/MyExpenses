package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import androidx.recyclerview.widget.DiffUtil
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*


data class Budget(val id: Long, val accountId: Long, val title: String, val description: String,
                  val currency: CurrencyUnit, val amount: Money, val grouping: Grouping, val color: Int) {

    fun toContentValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE, title)
        contentValues.put(KEY_DESCRIPTION, description)
        contentValues.put(KEY_GROUPING, grouping.name)
        contentValues.put(KEY_BUDGET, amount.amountMinor)
        if (accountId > 0) {
            contentValues.put(KEY_ACCOUNTID, accountId)
            contentValues.putNull(KEY_CURRENCY)
        } else {
            contentValues.put(KEY_CURRENCY, currency.code())
            contentValues.putNull(KEY_ACCOUNTID)
        }
        return contentValues
    }


    companion object {
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
