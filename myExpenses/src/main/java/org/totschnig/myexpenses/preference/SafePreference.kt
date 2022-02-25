package org.totschnig.myexpenses.preference

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.Preference

class SafePreference constructor(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs) {
    @SuppressLint("RestrictedApi")
    override fun performClick() {
        try {
            super.performClick()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No browser installed", Toast.LENGTH_LONG).show()
        }
    }
}