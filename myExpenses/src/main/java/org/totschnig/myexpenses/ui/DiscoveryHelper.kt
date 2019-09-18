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
import javax.inject.Inject

class DiscoveryHelper @Inject constructor(val prefHandler: PrefHandler) {
    fun discover(context: Activity, target: View, title: CharSequence, description: CharSequence,
                 daysSinceInstall: Int, prefKey: String, measureTarget: Boolean) =
            Utils.getDaysSinceInstall(context) >= daysSinceInstall && prefHandler.getBoolean(prefKey, true).also {
                if (it) {
                    if (measureTarget) {
                        target.getViewTreeObserver().addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                discoveryShow(context, target, title, description, prefKey, UiUtils.px2Dp(target.width / 2))
                                target.getViewTreeObserver().removeGlobalOnLayoutListener(this)
                            }
                        })
                    } else {
                        discoveryShow(context, target, title, description, prefKey)
                    }
                }
            }

    fun markDiscovered(prefKey: String) {
        prefHandler.putBoolean(prefKey, false)
    }

    private fun discoveryShow(context: Activity, target: View, title: CharSequence, description: CharSequence,
                              prefKey: String, targetRadius: Int? = null) {
        TapTargetView.showFor(context,
                TapTarget.forView(target, title, description)
                        .transparentTarget(true)
                        .outerCircleColorInt(UiUtils.themeIntAttr(context, R.attr.colorAccent))
                        .apply {
                            targetRadius?.let { this.targetRadius(it) }
                        },
                object : TapTargetView.Listener() {
                    override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                        markDiscovered(prefKey)
                    }
                })
    }
}