package org.totschnig.myexpenses.util

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import org.totschnig.myexpenses.util.ui.findParentWithTypeRecursively
import org.totschnig.myexpenses.util.ui.resolveThemeColor

fun linkInputsWithLabels(table: TableLayout) {
    val listener = OnFocusChangeListener { v: View, hasFocus: Boolean ->
        (findParentWithTypeRecursively(v, TableRow::class.java)?.getChildAt(0) as? TextView)?.apply {
            setTextColor(if (hasFocus) readPrimaryColor(table.context) else readPrimaryTextColor(table.context))
            setTypeface(null, if (hasFocus) Typeface.BOLD else Typeface.NORMAL)
        }
    }
    for (i in 0 until table.childCount) {
        (table.getChildAt(i) as? TableRow)?.let {
            for (j in 1 until it.childCount) {
                setOnFocusChangeListenerRecursive(it.getChildAt(j), listener)
            }
        }
    }
}

fun readPrimaryTextColor(context: Context) = resolveThemeColor(context, android.R.attr.textColorPrimary)

fun readPrimaryColor(context: Context) = resolveThemeColor(context, android.R.attr.colorPrimary)

private fun setOnFocusChangeListenerRecursive(view: View, listener: OnFocusChangeListener) {
    if (view is ViewGroup && view !is Spinner && (!view.isFocusable || view.descendantFocusability == ViewGroup.FOCUS_AFTER_DESCENDANTS)) {
        for (i in 0 until view.childCount) {
            setOnFocusChangeListenerRecursive(view.getChildAt(i), listener)
        }
    } else {
        view.onFocusChangeListener = listener
    }
}