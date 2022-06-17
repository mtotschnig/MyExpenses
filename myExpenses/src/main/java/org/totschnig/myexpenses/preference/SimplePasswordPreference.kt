package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.totschnig.myexpenses.R

class SimplePasswordPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {

    private fun init() {
        dialogLayoutResource = R.layout.simple_password_dialog
    }

    var value: String?
        get() = getPersistedString(null)
        set(value) {
            persistString(value)
        }

    init {
        init()
    }
}