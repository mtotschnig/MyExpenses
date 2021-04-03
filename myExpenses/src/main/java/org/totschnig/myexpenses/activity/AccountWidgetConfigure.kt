package org.totschnig.myexpenses.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.totschnig.myexpenses.R


class AccountWidgetConfigure : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_widget_configure)
        setResult(RESULT_CANCELED)
    }

    fun createWidget(view: View) {
        val appWidgetId = intent.extras!!.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_OK, Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        })
        finish()
    }
}