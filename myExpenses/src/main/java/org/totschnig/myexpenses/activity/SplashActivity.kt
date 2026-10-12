package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.isOnboardingCompleted
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET
import org.totschnig.myexpenses.widget.EXTRA_START_FROM_WIDGET_DATA_ENTRY
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as? MyApplication)?.also {
            val prefHandler = it.appComponent.prefHandler()
            val targetIntent = if (prefHandler.isOnboardingCompleted) {
                if (intent.getBooleanExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, false)) {
                    Intent(this, ExpenseEdit::class.java).apply {
                        putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true)
                        putExtra(EXTRA_START_FROM_WIDGET, true)
                        putExtra(EXTRA_START_FROM_WIDGET_DATA_ENTRY, true)
                    }
                } else {
                    Intent(this, MyExpensesV2::class.java)
                }
            } else {
                Intent(this, OnboardingActivity::class.java)
            }
            startActivity(targetIntent.apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
        } ?: run {
            Handler().post { exitProcess(0) }
        }
        finish()
    }
}