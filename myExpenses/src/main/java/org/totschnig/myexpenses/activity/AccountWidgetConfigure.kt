package org.totschnig.myexpenses.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.totschnig.myexpenses.databinding.AccountWidgetConfigureBinding
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.updateWidgets


class AccountWidgetConfigure : AppCompatActivity() {
    private lateinit var binding: AccountWidgetConfigureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AccountWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)
        binding.btnApply.setOnClickListener {
            appWidgetId?.let {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, it)
                })
                updateWidgets(this, AccountWidget::class.java, AppWidgetManager.ACTION_APPWIDGET_UPDATE, intArrayOf(it))
            }
            finish()
        }
    }

    val appWidgetId: Int?
        get() = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
}