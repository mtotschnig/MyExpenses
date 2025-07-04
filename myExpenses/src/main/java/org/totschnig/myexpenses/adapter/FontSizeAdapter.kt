package org.totschnig.myexpenses.adapter

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.R
import org.totschnig.myexpenses.preference.FontSizeDialogPreference
import timber.log.Timber

class FontSizeAdapter(context: Context) : ArrayAdapter<String?>(
    context,
    getItemLayoutResourceId(context),
    FontSizeDialogPreference.getEntries(context)
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent) as TextView
        updateTextView(row, position)
        return row
    }

    companion object {
        private fun getItemLayoutResourceId(context: Context): Int {
            val a = context.obtainStyledAttributes(
                null, R.styleable.AlertDialog,
                R.attr.alertDialogStyle, 0
            )
            val resId = a.getResourceId(R.styleable.AlertDialog_singleChoiceItemLayout, 0)
            a.recycle()
            return resId
        }

        fun updateTextView(textView: TextView, value: Int) {
            val base = 30f // We can take a random base, the important thing is that it is not dependent on the current fontscale
            val factor = 1 + value / 10f
            val size = base * factor
            Timber.d("updateTextView: size: %s", size)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        }
    }
}
