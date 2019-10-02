package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import androidx.recyclerview.widget.DiffUtil
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants.*


data class Budget(val id: Long, val accountId: Long, val title: String, val description: String,
                  val currency: CurrencyUnit, val amount: Money, val grouping: Grouping, val color: Int,
                  val start: LocalDate?, val end: LocalDate?) {
    constructor(id: Long, accountId: Long, title: String, description: String, currency: CurrencyUnit, amount: Money, grouping: Grouping, color: Int, start: String?, end: String?) : this(
            id, accountId, title, description, currency, amount, grouping, color, LocalDate.parse(start), LocalDate.parse(end)
    )

    init {
        when(grouping) {
            Grouping.NONE -> if (start == null || end == null) throw IllegalArgumentException("start and date are required with Grouping.NONE")
            else -> if (start != null || end != null) throw IllegalArgumentException("start and date are only allowed with Grouping.NONE")
        }
    }

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
        if (grouping == Grouping.NONE) {
            contentValues.put(KEY_START, start!!.format(ISO_LOCAL_DATE))
            contentValues.put(KEY_END, end!!.format(ISO_LOCAL_DATE))
        } else {
            contentValues.putNull(KEY_START)
            contentValues.putNull(KEY_END)
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
