package org.totschnig.myexpenses.ui

import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import timber.log.Timber
import javax.inject.Inject

class DiscoveryHelper @Inject constructor(val prefHandler: PrefHandler) {

    enum class Feature(val key: String) {
        EI_SWITCH("showDiscoveryExpenseIncomeSwitch"),
        OPERATION_TYPE_SELECT("showDiscoveryOperationTypeSelect")
    }

    fun discover(context: Activity, target: View, title: CharSequence, description: CharSequence,
                 daysSinceInstall: Int, feature: Feature, measureTarget: Boolean) =
            Utils.getDaysSinceInstall(context) >= daysSinceInstall && prefHandler.getBoolean(feature.key, true).also {
                if (it) {
                    if (measureTarget) {
                        target.getViewTreeObserver().addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                discoveryShow(context, target, title, description, feature, UiUtils.px2Dp(target.width / 2))
                                target.getViewTreeObserver().removeGlobalOnLayoutListener(this)
                            }
                        })
                    } else {
                        discoveryShow(context, target, title, description, feature)
                    }
                }
            }

    fun markDiscovered(feature: Feature) {
        prefHandler.putBoolean(feature.key, false)
        Timber.d("Marked as discoverd: %s", feature)
    }

    private fun discoveryShow(context: Activity, target: View, title: CharSequence, description: CharSequence,
                              feature: Feature, targetRadius: Int? = null) {
        TapTargetView.showFor(context,
                TapTarget.forView(target, title, description)
                        .transparentTarget(true)
                        .outerCircleColorInt(UiUtils.themeIntAttr(context, R.attr.colorAccent))
                        .apply {
                            targetRadius?.let { this.targetRadius(it) }
                        },
                object : TapTargetView.Listener() {
                    override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                        markDiscovered(feature)
                    }
                })
    }
}