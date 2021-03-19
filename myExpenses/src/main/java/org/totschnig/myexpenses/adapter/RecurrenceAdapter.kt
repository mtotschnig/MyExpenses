package org.totschnig.myexpenses.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Plan.Recurrence

class RecurrenceAdapter(context: Context?) : ArrayAdapter<Recurrence?>(context!!, android.R.layout.simple_spinner_item,
        arrayOf(Recurrence.NONE, Recurrence.ONETIME, Recurrence.DAILY, Recurrence.WEEKLY, Recurrence.MONTHLY, Recurrence.YEARLY, Recurrence.CUSTOM)) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = super.getView(position, convertView, parent)
        (result as TextView).text = getItem(position)!!.getLabel()
        return result
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = super.getDropDownView(position, convertView, parent)
        (result as TextView).text = getItem(position)!!.getLabel()
        return result
    }

    fun Recurrence.getLabel(): String {
        return when (this) {
            Recurrence.ONETIME -> context.getString(R.string.does_not_repeat)
            Recurrence.DAILY -> context.getString(R.string.daily_plain)
            Recurrence.WEEKLY -> context.getString(R.string.weekly_plain)
            Recurrence.MONTHLY -> context.getString(R.string.monthly_plain)
            Recurrence.YEARLY -> context.getString(R.string.yearly_plain)
            Recurrence.CUSTOM -> context.getString(R.string.pref_sort_order_custom)
            else -> "- - - -"
        }
    }

    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }
}