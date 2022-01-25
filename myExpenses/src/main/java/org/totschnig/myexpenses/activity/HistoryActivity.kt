package org.totschnig.myexpenses.activity

import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import android.os.Bundle
import org.totschnig.myexpenses.R

class HistoryActivity : ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history)
        setupToolbar(true)
    }
}