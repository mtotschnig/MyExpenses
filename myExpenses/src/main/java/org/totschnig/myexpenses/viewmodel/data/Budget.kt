package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentValues
import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.FormatStyle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.util.toEndOfDayEpoch
import org.totschnig.myexpenses.util.toStartOfDayEpoch


data class Budget(
    val id: Long, override val accountId: Long, val title: String, val description: String?,
    override val currency: CurrencyUnit, val grouping: Grouping, override val color: Int,
    val start: LocalDate?, val end: LocalDate?, val accountName: String?, val default: Boolean) : DistributionAccountInfo {
    constructor(id: Long, accountId: Long, title: String, description: String?, currency: CurrencyUnit, grouping: Grouping, color: Int, start: String?, end: String?, accountName: String?, default: Boolean) : this(
            id, accountId, title, description, currency, grouping, color, start?.let { LocalDate.parse(it) }, end?.let { LocalDate.parse(it) }, accountName, default)

    init {
        when (grouping) {
            Grouping.NONE -> if (start == null || end == null) throw IllegalArgumentException("start and date are required with Grouping.NONE")
            else -> if (start != null || end != null) throw IllegalArgumentException("start and date are only allowed with Grouping.NONE")
        }
    }

    override fun label(context: Context) = accountName
            ?: if (accountId == Account.HOME_AGGREGATE_ID) context.getString(R.string.grand_total)
            else currency.code

    /**
     * @param budget We add the initial budget to the content values,
     * since this is what TransactionProvider expects
     */
    fun toContentValues(budget: Long?) = ContentValues().apply {
        put(KEY_TITLE, title)
        put(KEY_DESCRIPTION, description)
        if (id == 0L) {
            put(KEY_GROUPING, grouping.name)
        }
        if (accountId > 0) {
            put(KEY_ACCOUNTID, accountId)
            putNull(KEY_CURRENCY)
        } else {
            put(KEY_CURRENCY, currency.code)
            putNull(KEY_ACCOUNTID)
        }
        if (grouping == Grouping.NONE) {
            put(KEY_START, startIso())
            put(KEY_END, endIso())
        } else {
            putNull(KEY_START)
            putNull(KEY_END)
        }
        budget?.let {
            put(KEY_BUDGET, it)
        }
    }

    private fun startIso(): String = start!!.format(ISO_LOCAL_DATE)
    private fun endIso(): String = end!!.format(ISO_LOCAL_DATE)
    fun durationAsSqlFilter() = "$KEY_DATE BETWEEN ${start!!.toStartOfDayEpoch()}  AND ${end!!.toEndOfDayEpoch()}"

    fun durationPrettyPrint(): String {
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        return "%s - %s".format(start!!.format(dateFormat), end!!.format(dateFormat))
    }

    fun titleComplete(context: Context) = "%s (%s)".format(title,
            when (grouping) {
                Grouping.NONE -> durationPrettyPrint()
                else -> context.getString(grouping.getLabelForBudgetType())
            }
    )

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

fun Grouping.getLabelForBudgetType() = when (this) {
    Grouping.DAY -> R.string.daily_plain
    Grouping.WEEK -> R.string.weekly_plain
    Grouping.MONTH -> R.string.monthly_plain
    Grouping.YEAR -> R.string.yearly_plain
    Grouping.NONE -> R.string.budget_onetime
}