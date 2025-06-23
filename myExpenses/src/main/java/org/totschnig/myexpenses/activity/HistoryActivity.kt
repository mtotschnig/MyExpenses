package org.totschnig.myexpenses.activity

import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.HistoryChart

class HistoryActivity : ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWithFragment(savedInstanceState == null, false) {
            HistoryChart()
        }
        setupToolbar()
    }

    override val drawToBottomEdge = false
}