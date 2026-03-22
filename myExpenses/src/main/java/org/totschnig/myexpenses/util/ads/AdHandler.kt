package org.totschnig.myexpenses.util.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.util.tracking.Tracker

private const val AD_TYPE_BANNER = "banner"
private const val AD_TYPE_INTERSTITIAL = "interstitial"
const val INTERSTITIAL_MIN_INTERVAL = 4

interface AdHandlerBase {

    fun trackBannerRequest(provider: String) {
        track(Tracker.EVENT_AD_REQUEST, AD_TYPE_BANNER, provider)
    }

    fun trackInterstitialRequest(provider: String) {
        track(Tracker.EVENT_AD_REQUEST, AD_TYPE_INTERSTITIAL, provider)
    }

    fun trackBannerLoaded(provider: String) {
        track(Tracker.EVENT_AD_LOADED, AD_TYPE_BANNER, provider)
    }

    fun trackInterstitialLoaded(provider: String) {
        track(Tracker.EVENT_AD_LOADED, AD_TYPE_INTERSTITIAL, provider)
    }

    fun trackBannerFailed(provider: String, errorCode: String?) {
        track(Tracker.EVENT_AD_FAILED, AD_TYPE_BANNER, provider, errorCode)
    }

    fun trackInterstitialFailed(provider: String, errorCode: String?) {
        track(Tracker.EVENT_AD_FAILED, AD_TYPE_INTERSTITIAL, provider, errorCode)
    }

    fun trackInterstitialShown(provider: String) {
        track(Tracker.EVENT_AD_SHOWN, AD_TYPE_INTERSTITIAL, provider)
    }

    fun buildBundle(type: String, provider: String, errorCode: String?): Bundle {
        val bundle = Bundle(if (errorCode == null) 2 else 3)
        bundle.putString(Tracker.EVENT_PARAM_AD_TYPE, type)
        bundle.putString(Tracker.EVENT_PARAM_AD_PROVIDER, provider)
        if (errorCode != null) {
            bundle.putString(Tracker.EVENT_PARAM_AD_ERROR_CODE, errorCode)
        }
        return bundle
    }

    fun track(event: String, type: String, provider: String, errorCode: String? = null) {}
}

interface AdHandler: AdHandlerBase {
    fun startBanner() {}

    fun maybeRequestNewInterstitial() {}

    fun maybeShowInterstitial(): Boolean = false

    fun onEditTransactionResult(): Boolean = false

    fun onResume() {}

    fun onDestroy() {}

    fun onPause() {}

}

interface AdHandlerV2: AdHandlerBase {
    fun maybeRequestNewInterstitial(context: Context) {}
    fun onEditTransactionResult(context: Activity): Boolean = false
    @Composable
    fun Banner() {}
}