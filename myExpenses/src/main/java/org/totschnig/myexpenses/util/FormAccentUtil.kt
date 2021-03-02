package org.totschnig.myexpenses.util

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import org.totschnig.myexpenses.R

fun linkInputsWithLabels(table: TableLayout) {
    val listener = OnFocusChangeListener { v: View, hasFocus: Boolean ->
        (findParentWithTypeRecursively(v, TableRow::class.java)?.getChildAt(0) as? TextView)?.setTextColor(
                if (hasFocus) readAccentColor(table.context) else readNormalColor(table.context))
    }
    for (i in 0 until table.childCount) {
        (table.getChildAt(i) as? TableRow)?.let {
            for (j in 1 until it.childCount) {
                setOnFocusChangeListenerRecursive(it.getChildAt(j), listener)
            }
        }
    }
}

fun readNormalColor(context: Context) = readThemeColor(context, android.R.attr.textColorSecondary)

fun readAccentColor(context: Context) = readThemeColor(context, R.attr.colorAccent)

fun readThemeColor(context: Context, attr: Int): Int {
    val typedValue = TypedValue()
    val a: TypedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(attr))
    val primaryColor = a.getColor(0, 0)
    a.recycle()
    return primaryColor
}

private fun setOnFocusChangeListenerRecursive(view: View, listener: OnFocusChangeListener) {
    if (view is ViewGroup && view !is Spinner && (!view.isFocusable() || view.descendantFocusability == ViewGroup.FOCUS_AFTER_DESCENDANTS)) {
        for (i in 0 until view.childCount) {
            setOnFocusChangeListenerRecursive(view.getChildAt(i), listener)
        }
    } else {
        view.onFocusChangeListener = listener
    }
}