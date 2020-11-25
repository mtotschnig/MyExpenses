package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.validateDateFormat
import java.text.SimpleDateFormat

class DateFormatPreference constructor(context: Context, attrs: AttributeSet) :
        LocalizedFormatEditTextPreference(context, attrs) {
    override fun getDefaultValue(): String? = (Utils.getFrameworkDateFormatSafe(context) as? SimpleDateFormat)?.toPattern()
    override fun validate(newValue: String) = validateDateFormat(newValue)
}