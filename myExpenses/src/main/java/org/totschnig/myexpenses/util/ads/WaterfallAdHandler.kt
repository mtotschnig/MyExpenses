package org.totschnig.myexpenses.util.ads

import android.view.ViewGroup
import org.totschnig.myexpenses.activity.BaseActivity

@Suppress("unused")
class WaterfallAdHandler internal constructor(factory: AdHandlerFactory, adContainer: ViewGroup, activity: BaseActivity, private vararg val cascade: BaseAdHandler) : BaseAdHandler(factory, adContainer, activity) {
    private var cascadingIndex = 0
    private var cascadingIndexInterstitial = 0
    public override fun startBannerInternal() {
        cascadingIndex = 0
        cascadingIndexInterstitial = 0
        startBannerCurrent()
    }

    override fun maybeShowInterstitialDo(): Boolean {
        //This method will be called directly on the children via onEditTransactionResult,
        return false
    }

    override fun requestNewInterstitialDo() {
        requestNewInterstitialCurrent()
    }

    private val current: BaseAdHandler?
        get() = if (cascadingIndex < cascade.size) cascade[cascadingIndex] else null
    private val currentForInterstitial: BaseAdHandler?
        get() = if (cascadingIndexInterstitial < cascade.size) cascade[cascadingIndexInterstitial] else null

    override fun hide() {
        cascadingIndex++
        startBannerCurrent()
    }

    private fun startBannerCurrent() {
        val current = current
        if (current == null) {
            super.hide()
        } else {
            current.startBanner()
        }
    }

    override fun onInterstitialFailed() {
        cascadingIndexInterstitial++
        requestNewInterstitialCurrent()
    }

    private fun requestNewInterstitialCurrent() {
        currentForInterstitial?.requestNewInterstitialDo()
    }

    override fun onEditTransactionResult() =
            currentForInterstitial?.onEditTransactionResult() ?: false

    override fun onResume() {
        val current = current
        current?.onResume()
    }

    override fun onDestroy() {
        val current = current
        current?.onDestroy()
    }

    override fun onPause() {
        val current = current
        current?.onPause()
    }

    init {
        for (child in cascade) {
            child.parent = this
        }
    }
}