package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.util.licence.BillingListener
import org.totschnig.myexpenses.util.licence.BillingManager

abstract class IapActivity: ProtectedFragmentActivity(), BillingListener {
    var billingManager: BillingManager? = null
    abstract val shouldQueryIap: Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = licenceHandler.initBillingManager(this, shouldQueryIap)
    }

    override fun onResume() {
        super.onResume()
        billingManager?.onResume(shouldQueryIap)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager?.destroy()
        billingManager = null
    }
}