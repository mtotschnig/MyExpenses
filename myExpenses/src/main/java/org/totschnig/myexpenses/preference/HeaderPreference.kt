package org.totschnig.myexpenses.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import org.totschnig.myexpenses.R

class HeaderPreference(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs) {
    init {
        layoutResource = R.layout.preference_header
        isVisible = false
    }
}