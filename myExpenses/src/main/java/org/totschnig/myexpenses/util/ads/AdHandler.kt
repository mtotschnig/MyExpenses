package org.totschnig.myexpenses.util.ads

interface AdHandler {
    fun startBanner() {}

    fun maybeRequestNewInterstitial() {}

    fun maybeShowInterstitial() {}

    fun onEditTransactionResult() {}

    fun onResume() {}

    fun onDestroy() {}

    fun onPause() {}
}