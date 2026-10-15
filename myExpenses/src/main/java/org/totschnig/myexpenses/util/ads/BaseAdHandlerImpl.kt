package org.totschnig.myexpenses.util.ads

import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.tracking.Tracker

@Suppress("SameParameterValue")
abstract class BaseAdHandlerImpl protected constructor(
    private val factory: AdHandlerFactory,
    protected val adContainer: ViewGroup,
    protected var activity: BaseActivity
) : AdHandler {
    val tracker: Tracker
        get() = activity.tracker
    val prefHandler: PrefHandler
        get() = activity.prefHandler
    var parent: BaseAdHandlerImpl? = null

    override fun startBanner() {
        try {
            if (shouldHideAd) {
                hide()
            } else {
                startBannerInternal()
            }
        } catch (e: Exception) {
            CrashHandler.report(e)
        }
    }

    protected abstract fun startBannerInternal()

    override fun maybeRequestNewInterstitial() {
        if (
            (prefHandler.getBoolean(PrefKey.DEBUG_ADS, false) || (
                    System.currentTimeMillis() - prefHandler.getLong(
                        PrefKey.INTERSTITIAL_LAST_SHOWN,
                        0
                    ) > DateUtils.MINUTE_IN_MILLIS * 10 &&
                            prefHandler.getInt(
                                PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL,
                                0
                            ) > INTERSTITIAL_MIN_INTERVAL
                    )) && !shouldHideAd
        ) {
            //last ad shown more than one hour and at least five expense entries ago,
            requestNewInterstitialDo()
        }
    }

    override fun maybeShowInterstitial() = if (maybeShowInterstitialDo()) {
        prefHandler.putLong(PrefKey.INTERSTITIAL_LAST_SHOWN, System.currentTimeMillis())
        prefHandler.putInt(PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0)
        true
    } else {
        prefHandler.putInt(
            PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL,
            prefHandler.getInt(PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0) + 1
        )
        maybeRequestNewInterstitial()
        false
    }

    abstract fun maybeShowInterstitialDo(): Boolean

    abstract fun requestNewInterstitialDo()

    open val shouldHideAd
        get() = factory.isAdDisabled

    protected open fun onInterstitialFailed() {
        if (parent != null) {
            parent!!.onInterstitialFailed()
        }
    }

    override fun onEditTransactionResult(): Boolean {
        try {
            if (!shouldHideAd) {
                return maybeShowInterstitial()
            }
        } catch (e: Exception) {
            CrashHandler.report(e)
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
}