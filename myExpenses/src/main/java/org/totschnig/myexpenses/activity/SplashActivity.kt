package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.maybeRepairRequerySchema

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefHandler = (application as MyApplication).appComponent.prefHandler()
        val version = prefHandler
            .getInt(PrefKey.CURRENT_VERSION, -1)
        if (!prefHandler.encryptDatabase && Build.VERSION.SDK_INT == 30 && version < 591) {
            maybeRepairRequerySchema(getDatabasePath("data").path)
            prefHandler.putBoolean(PrefKey.DB_SAFE_MODE, false)
            getStarted(version)
        } else {
            getStarted(version)
        }
    }

    fun getStarted(version: Int) {
        val intent = Intent(this, if (version == -1) OnboardingActivity::class.java else  MyExpenses::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        finish()
    }
}