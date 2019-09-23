package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.edit_budget.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Grouping

class EditBudgetDialog: CommitSafeDialogFragment(), DialogInterface.OnClickListener {
    override fun onClick(dialog: DialogInterface?, which: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
       val view = requireActivity().layoutInflater.inflate(R.layout.edit_budget, null)
        return AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle(R.string.menu_create_budget)
                .setPositiveButton(android.R.string.ok, this)
                .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Type.adapter = GroupingAdapter(requireContext())
    }
}

class GroupingAdapter(context: Context) : ArrayAdapter<Grouping>(context, android.R.layout.simple_spinner_item, android.R.id.text1, Grouping.values()) {

    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)
        setText(position, row)
        return row
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getDropDownView(position, convertView, parent)
        setText(position, row)
        return row
    }

    private fun setText(position: Int, row: View) {
        (row.findViewById<View>(android.R.id.text1) as TextView).setText(getBudgetLabelForSpinner(getItem(position)!!))
    }

    private fun getBudgetLabelForSpinner(type: Grouping) = when (type) {
        Grouping.DAY -> R.string.daily_plain
        Grouping.WEEK -> R.string.weekly_plain
        Grouping.MONTH -> R.string.monthly
        Grouping.YEAR -> R.string.yearly_plain
        Grouping.NONE -> R.string.budget_onetime
    }
}