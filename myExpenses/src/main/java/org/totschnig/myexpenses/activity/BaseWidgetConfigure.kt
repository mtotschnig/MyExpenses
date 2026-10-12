package org.totschnig.myexpenses.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import org.totschnig.myexpenses.preference.isOnboardingCompleted
import org.totschnig.myexpenses.widget.BaseWidget
import org.totschnig.myexpenses.widget.updateWidgets

abstract class BaseWidgetConfigure : ProtectedFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prefHandler.isOnboardingCompleted) {
            startActivity(Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }
    }

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