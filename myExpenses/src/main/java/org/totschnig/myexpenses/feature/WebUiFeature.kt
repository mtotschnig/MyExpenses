package org.totschnig.myexpenses.feature

import android.content.Context

interface WebUiFeature {
    fun bind(context: Context) {}
    fun unbind(context: Context) {}
    fun toggle(context: Context) {}

    val isBoundAndRunning: Boolean
        get() = false
}