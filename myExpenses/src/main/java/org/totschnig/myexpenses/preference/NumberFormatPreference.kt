package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils
import java.text.DecimalFormat
import java.text.NumberFormat

class NumberFormatPreference constructor(context: Context, attrs: AttributeSet) :
        LocalizedFormatEditTextPreference(context, attrs) {
    override fun getDefaultValue(): String? = (NumberFormat.getCurrencyInstance(Utils.localeFromContext(context)) as DecimalFormat).toLocalizedPattern()
    override fun validate(newValue: String) = try {
        DecimalFormat().applyLocalizedPattern(newValue)
        null
    } catch (e: IllegalArgumentException) {
        R.string.number_format_illegal
    }
}