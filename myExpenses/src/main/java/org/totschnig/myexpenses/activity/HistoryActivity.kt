package org.totschnig.myexpenses.activity

import android.os.Bundle
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