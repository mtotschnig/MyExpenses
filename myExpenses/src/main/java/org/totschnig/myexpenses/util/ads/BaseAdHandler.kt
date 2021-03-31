package org.totschnig.myexpenses.util.ads

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.tracking.Tracker
import timber.log.Timber

@Suppress("SameParameterValue")
abstract class BaseAdHandler protected constructor(private val factory: AdHandlerFactory, protected val adContainer: ViewGroup, protected var activity: BaseActivity) : AdHandler {
    val tracker: Tracker
        get() = activity.tracker
    val prefHandler: PrefHandler
        get() = activity.prefHandler
    var parent: BaseAdHandler? = null
    private var initialized = false
    protected fun init() {
        if (!initialized) {
            initInternal()
            initialized = true
        }
    }

    override fun startBanner() {
        try {
            init()
            if (shouldHideAd()) {
                hide()
            } else {
                startBannerInternal()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    protected open fun initInternal() {}

    protected abstract fun startBannerInternal()

    override fun maybeRequestNewInterstitial() {
        val now = System.currentTimeMillis()
        if (now - prefHandler.getLong(PrefKey.INTERSTITIAL_LAST_SHOWN, 0) > DateUtils.MINUTE_IN_MILLIS * 10 &&
                prefHandler.getInt(PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0) > INTERSTITIAL_MIN_INTERVAL) {
            //last ad shown more than one hour and at least five expense entries ago,
            requestNewInterstitialDo()
        }
    }

    override fun maybeShowInterstitial() = if (maybeShowInterstitialDo()) {
        prefHandler.putLong(PrefKey.INTERSTITIAL_LAST_SHOWN, System.currentTimeMillis())
        prefHandler.putInt(PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0)
        true
    } else {
        prefHandler.putInt(PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL,
                prefHandler.getInt(PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0) + 1)
        maybeRequestNewInterstitial()
        false
    }

    abstract fun maybeShowInterstitialDo(): Boolean

    abstract fun requestNewInterstitialDo()

    fun shouldHideAd(): Boolean {
        return factory.isAdDisabled || factory.isRequestLocationInEeaOrUnknown && !prefHandler.isSet(PrefKey.PERSONALIZED_AD_CONSENT)
    }

    protected open fun onInterstitialFailed() {
        if (parent != null) {
            parent!!.onInterstitialFailed()
        }
    }

    override fun onEditTransactionResult(): Boolean {
        try {
            if (!shouldHideAd()) {
                init()
                return maybeShowInterstitial()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return false
    }

    protected open fun hide() {
        if (parent != null) {
            parent!!.hide()
        } else {
            adContainer.visibility = View.GONE
        }
    }

    protected fun trackBannerRequest(provider: String) {
        track(Tracker.EVENT_AD_REQUEST, AD_TYPE_BANNER, provider)
    }

    protected fun trackInterstitialRequest(provider: String) {
        track(Tracker.EVENT_AD_REQUEST, AD_TYPE_INTERSTITIAL, provider)
    }

    protected fun trackBannerLoaded(provider: String) {
        track(Tracker.EVENT_AD_LOADED, AD_TYPE_BANNER, provider)
    }

    protected fun trackInterstitialLoaded(provider: String) {
        track(Tracker.EVENT_AD_LOADED, AD_TYPE_INTERSTITIAL, provider)
    }

    protected fun trackBannerFailed(provider: String, errorCode: String?) {
        track(Tracker.EVENT_AD_FAILED, AD_TYPE_BANNER, provider, errorCode)
    }

    protected fun trackInterstitialFailed(provider: String, errorCode: String?) {
        track(Tracker.EVENT_AD_FAILED, AD_TYPE_INTERSTITIAL, provider, errorCode)
    }

    protected fun trackInterstitialShown(provider: String) {
        track(Tracker.EVENT_AD_SHOWN, AD_TYPE_INTERSTITIAL, provider)
    }

    private fun track(event: String, type: String, provider: String, errorCode: String? = null) {
        tracker.logEvent(event, buildBundle(type, provider, errorCode))
    }

    private fun buildBundle(type: String, provider: String, errorCode: String?): Bundle {
        val bundle = Bundle(if (errorCode == null) 2 else 3)
        bundle.putString(Tracker.EVENT_PARAM_AD_TYPE, type)
        bundle.putString(Tracker.EVENT_PARAM_AD_PROVIDER, provider)
        if (errorCode != null) {
            bundle.putString(Tracker.EVENT_PARAM_AD_ERROR_CODE, errorCode)
        }
        return bundle
    }

    companion object {
        private const val INTERSTITIAL_MIN_INTERVAL = 4
        private const val AD_TYPE_BANNER = "banner"
        private const val AD_TYPE_INTERSTITIAL = "interstitial"
    }
}