package org.totschnig.myexpenses.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CrStatus.Companion.editableStatuses

class CrStatusAdapter(context: Context) : ArrayAdapter<CrStatus?>(
    context,
    R.layout.spinner_item_with_color,
    TEXT_VIEW_RESOURCE_ID,
    editableStatuses
) {
    init {
        setDropDownViewResource(R.layout.spinner_dropdown_item_with_color)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)
        getItem(position)?.let {
            setColor(it, row)
        }
        return row
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getDropDownView(position, convertView, parent)
        getItem(position)?.let {
            setColor(it, row)
            row.findViewById<TextView>(TEXT_VIEW_RESOURCE_ID).setText(it.toStringRes())
        }
        return row
    }

    private fun setColor(item: CrStatus, row: View) {
        row.findViewById<View>(R.id.color1).setBackgroundColor(item.toColorRoles(context).accent)
    }

    companion object {
        private const val TEXT_VIEW_RESOURCE_ID = android.R.id.text1
    }
}
