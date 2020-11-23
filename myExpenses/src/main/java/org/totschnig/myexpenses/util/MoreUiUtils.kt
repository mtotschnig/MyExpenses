package org.totschnig.myexpenses.util

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils.calculateContrast
import com.google.android.material.chip.ChipGroup
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.filter.ScrollingChip

fun <T> ChipGroup.addChipsBulk(chips: Iterable<T>, closeFunction: ((T) -> Unit)?) {
    removeAllViews()
    for (chip in chips) {
        addView(ScrollingChip(context).also { scrollingChip ->
            scrollingChip.text = chip.toString()
            closeFunction?.let {
                scrollingChip.isCloseIconVisible = true
                scrollingChip.setOnCloseIconClickListener {
                    removeView(scrollingChip)
                    it(chip)
                }
            }
        })
    }
}

fun setNightMode(prefHandler: PrefHandler, context: Context) {
    AppCompatDelegate.setDefaultNightMode(
            when (prefHandler.getString(PrefKey.UI_THEME_KEY, context.getString(R.string.pref_ui_theme_default))) {
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
}

fun linkInputsWithLabels(table: TableLayout) {
    val primaryColor = readNormalColor(table.context)
    val accentColor = readAccentColor(table.context)
    for (i in 0 until table.childCount) {
        (table.getChildAt(i) as? TableRow)?.let {
            val label = it.getChildAt(0)
            for (j in 1 until it.childCount) {
                linkInputWithLabel(it.getChildAt(j), label, accentColor, primaryColor)
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

fun linkInputWithLabel(input: View, label: View) {
    linkInputWithLabel(input, label, readAccentColor(input.context), readNormalColor(input.context))
}

fun linkInputWithLabel(input: View, label: View, accentColor: Int, normalColor: Int) {
    setOnFocusChangeListenerRecursive(input) { v: View?, hasFocus: Boolean -> (label as TextView).setTextColor(if (hasFocus) accentColor else normalColor) }
}

private fun setOnFocusChangeListenerRecursive(view: View, listener: OnFocusChangeListener) {
    if (view is ViewGroup && (!view.isFocusable() || view.descendantFocusability == ViewGroup.FOCUS_AFTER_DESCENDANTS)) {
        for (i in 0 until view.childCount) {
            setOnFocusChangeListenerRecursive(view.getChildAt(i), listener)
        }
    } else {
        view.onFocusChangeListener = listener
    }
}

fun getBestForeground(color: Int) = arrayOf(Color.BLACK, Color.WHITE).maxByOrNull { calculateContrast(color, it) }!!

inline fun <reified E : Enum<E>> getEnumFromPreferencesWithDefault(prefHandler: PrefHandler, prefKey: PrefKey, defaultValue: E) =
        try {
            enumValueOf(prefHandler.getString(prefKey, defaultValue.name)!!)
        } catch (e: IllegalArgumentException) {
            defaultValue
        }