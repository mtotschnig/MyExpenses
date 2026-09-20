package org.totschnig.myexpenses.widget

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper

class WidgetObserver(
    private val context: Context,
    private val mProvider: Class<out BaseWidget>
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = Runnable {
        updateWidgets(context, mProvider, WIDGET_LIST_DATA_CHANGED)
    }

    override fun onChange(selfChange: Boolean) {
        mainHandler.removeCallbacks(updateRunnable)
        mainHandler.postDelayed(updateRunnable, DEBOUNCE_DELAY_MS)
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 500L

        fun register(context: Context) {
            register(context, TemplateWidget::class.java, TemplateWidget.OBSERVED_URIS)
            register(context, AccountWidget::class.java, AccountWidget.OBSERVED_URIS)
            register(context, BudgetWidget::class.java, BudgetWidget.OBSERVED_URIS)
        }

        private fun register(context: Context, mProvider: Class<out BaseWidget>, observedUris: Array<Uri>) {
            WidgetObserver(context, mProvider).apply {
                for (uri in observedUris) {
                    context.contentResolver.registerContentObserver(uri, true, this)
                }
            }
        }
    }
}
