package org.totschnig.myexpenses.ui

import android.content.Context
import android.util.AttributeSet
import timber.log.Timber

class TimePicker(context: Context, attrs: AttributeSet?) : android.widget.TimePicker(context, attrs) {
    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        try {
            super.onRtlPropertiesChanged(layoutDirection)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}