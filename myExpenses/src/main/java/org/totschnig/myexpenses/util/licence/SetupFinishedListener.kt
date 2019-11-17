package org.totschnig.myexpenses.util.licence

interface SetupFinishedListener {
    fun onBillingSetupFinished()
    fun onBillingSetupFailed(reason: String)
}
