package org.totschnig.myexpenses.util.ads

interface AdHandler {
    fun startBanner() {}

    fun maybeRequestNewInterstitial() {}

    fun maybeShowInterstitial(): Boolean = false

    fun onEditTransactionResult(): Boolean = false

    fun onResume() {}

    fun onDestroy() {}

    fun onPause() {}
}