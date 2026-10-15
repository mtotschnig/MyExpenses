package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.totschnig.myexpenses.R

class LegacyPasswordPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private var valueSet = false

    init {
        dialogLayoutResource = R.layout.password_dialog
    }

    fun setValue(value: Boolean) {
        val oldValue = getValue()
        val changed = value != oldValue
        if (changed || !valueSet) {
            valueSet = true
            persistBoolean(value)
            if (changed) {
                notifyChanged()
            }
        }
    }

    fun getValue() = getPersistedBoolean(false)
}
