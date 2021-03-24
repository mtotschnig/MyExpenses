package org.totschnig.myexpenses.util.ads

import android.view.View
import android.view.ViewGroup
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.util.ads.customevent.AdListener
import org.totschnig.myexpenses.util.ads.customevent.AdView
import org.totschnig.myexpenses.util.ads.customevent.Interstitial
import org.totschnig.myexpenses.util.ads.customevent.PartnerProgram
import kotlin.math.roundToInt

class CustomAdHandler(factory: AdHandlerFactory, adContainer: ViewGroup, baseActivity: BaseActivity, private val userCountry: String) : BaseAdHandler(factory, adContainer, baseActivity) {
    private var adView: AdView? = null
    private var interstitial: Interstitial? = null
    private var mInterstitialShown = false
    public override fun startBannerInternal() {
        val density = activity.resources.displayMetrics.density
        val contentProvider = PartnerProgram.pickContent(
                listOf(*PartnerProgram.values()),
                userCountry,
                activity,
                (adContainer.width / density).roundToInt())
        if (contentProvider == null) {
            hide()
        } else {
            adView = AdView(activity).apply {
                setAdListener(object : AdListener() {
                    override fun onBannerLoaded(view: View) {
                        adContainer.addView(view)
                    }
                })
                fetchAd(contentProvider)
            }
        }
    }

    override fun maybeShowInterstitialDo() = if (mInterstitialShown) false else interstitial?.let {
        it.show()
        mInterstitialShown = true
        true
    } ?: false

    override fun requestNewInterstitialDo() {
        val contentProvider = PartnerProgram.pickContent(listOf(*PartnerProgram.values()),
                userCountry, activity, -1)
        if (contentProvider != null) {
            mInterstitialShown = false
            interstitial = Interstitial(activity).apply {
                setContentProvider(contentProvider)
                setAdListener(object : AdListener() {})
                fetchAd()
            }
        } else {
            onInterstitialFailed()
        }
    }

    override fun onDestroy() {
        if (adView != null) {
            adView!!.destroy()
        }
    }
}