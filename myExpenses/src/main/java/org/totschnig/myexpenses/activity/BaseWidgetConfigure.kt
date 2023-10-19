package org.totschnig.myexpenses.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.totschnig.myexpenses.widget.BaseWidget
import org.totschnig.myexpenses.widget.updateWidgets

abstract class BaseWidgetConfigure: AppCompatActivity() {
    fun apply(clazz: Class<out BaseWidget>) {
        appWidgetId?.let {
            setResult(RESULT_OK, Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, it)
            })
            updateWidgets(this, clazz, AppWidgetManager.ACTION_APPWIDGET_UPDATE, intArrayOf(it))
        }
        finish()
    }

    val appWidgetId: Int?
        get() = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
}