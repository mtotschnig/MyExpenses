package org.totschnig.myexpenses.adapter

import android.content.Context
import android.provider.Settings
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
        row.updateTextSize(context, position)
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

        fun TextView.updateTextSize(context: Context, value: Int) {
            val systemFontScale = Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1.0f)
            val base = 14f
            val factor = 1 + value / 10f
            val size = base * systemFontScale * factor
            Timber.d("updateTextView: systemFontScale %f, factor: %f, size: %f", systemFontScale, factor, size)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        }
    }
}
