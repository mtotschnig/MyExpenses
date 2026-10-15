package org.totschnig.myexpenses.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.totschnig.myexpenses.db2.entities.Recurrence

class RecurrenceAdapter(context: Context?) : ArrayAdapter<Recurrence?>(context!!, android.R.layout.simple_spinner_item,
        arrayOf(Recurrence.NONE, Recurrence.ONETIME, Recurrence.DAILY, Recurrence.WEEKLY, Recurrence.MONTHLY, Recurrence.YEARLY, Recurrence.CUSTOM)) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = super.getView(position, convertView, parent)
        (result as TextView).text = getItem(position)!!.label(context)
        return result
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val result = super.getDropDownView(position, convertView, parent)
        (result as TextView).text = getItem(position)!!.label(context)
        return result
    }

    init {
        setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }
}