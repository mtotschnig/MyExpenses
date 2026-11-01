package org.totschnig.myexpenses.preference

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import org.totschnig.myexpenses.injector

class SecureEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : EditTextPreference(context, attrs) {

    init {
        preferenceDataStore = context.injector.securePreferenceDataStore()

        attrs?.let {
            val a = context.obtainStyledAttributes(it, intArrayOf(android.R.attr.inputType))
            val inputType = a.getInt(0, InputType.TYPE_CLASS_TEXT)
            a.recycle()
            if (inputType != InputType.TYPE_CLASS_TEXT) {
                setOnBindEditTextListener { editText ->
                    editText.inputType = inputType
                }
            }
        }
    }
}
