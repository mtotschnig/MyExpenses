package org.totschnig.myexpenses.util.licence

interface BillingListener {
    fun onBillingSetupFinished()
    fun onBillingSetupFailed(reason: String)
    fun onLicenceStatusSet(newStatus: String?)
}
