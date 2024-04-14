package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

/**
 * a Preference that allows to store a value. All functionality is driven by the consumer fragment.
 */
class SimpleValuePreference(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs) {
    var value: String = ""
        set(value) {
            field = value
            persistString(value)
            notifyChanged()
        }
}