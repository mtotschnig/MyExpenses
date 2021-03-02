package org.totschnig.myexpenses.widget

import android.content.Context
import android.database.ContentObserver
import android.net.Uri

class WidgetObserver(private val context: Context, private val mProvider: Class<out AbstractWidget>) : ContentObserver(null) {
    override fun onChange(selfChange: Boolean) {
        updateWidgets(context, mProvider, WIDGET_LIST_DATA_CHANGED)
    }
    companion object {
        fun register(context: Context) {
            register(context, TemplateWidget::class.java, TemplateWidget.OBSERVED_URIS)
            register(context, AccountWidget::class.java, AccountWidget.OBSERVED_URIS)
        }
        private fun register(context: Context, mProvider: Class<out AbstractWidget>, observedUris: Array<Uri>) {
            WidgetObserver(context, mProvider).apply {
                for (uri in observedUris) {
                    context.contentResolver.registerContentObserver(uri, true, this)
                }
            }
        }
    }
}