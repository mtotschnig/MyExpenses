package org.totschnig.myexpenses.preference

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class EditNumberPreference(context: Context, attrs: AttributeSet) :
    EditTextPreference(context, attrs) {

    init {
        setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
    override fun onSetInitialValue(defaultValue: Any?) {
        text = getPersistedInt(defaultValue as? Int ?: 0).toString()
    }

    override fun persistString(value: String?): Boolean {
        if (!shouldPersist()) {
            return false
        }

        value?.toInt()?.let { persistInt(it) }
        return true
    }
}