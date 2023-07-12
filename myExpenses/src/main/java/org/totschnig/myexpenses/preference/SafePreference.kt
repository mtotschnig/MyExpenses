package org.totschnig.myexpenses.preference

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.Preference
import org.totschnig.myexpenses.util.safeMessage
import java.lang.Exception

class SafePreference(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs) {
    @SuppressLint("RestrictedApi")
    override fun performClick() {
        try {
            super.performClick()
        } catch (e: Exception) {
            Toast.makeText(context, e.safeMessage, Toast.LENGTH_LONG).show()
        }
    }
}