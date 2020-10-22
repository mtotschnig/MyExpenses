package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.totschnig.myexpenses.preference.PrefKey

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, if (PrefKey.CURRENT_VERSION.getInt(-1) == -1) OnboardingActivity::class.java else  MyExpenses::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
        return
    }
}