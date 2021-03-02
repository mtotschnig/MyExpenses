package org.totschnig.myexpenses.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import timber.log.Timber

class DiscoveryHelper(val prefHandler: PrefHandler) : IDiscoveryHelper {

    enum class Feature(val key: String) {
        expense_income_switch("showDiscoveryExpenseIncomeSwitch") {
            override fun toTitle(context: Context) = with(context) {
                String.format("%s / %s", getString(R.string.expense), getString(R.string.income))
            }
        },
        fab_long_press("showDiscoveryFabLongPress") {
            override fun toTitle(context: Context) = with(context) {
                String.format("%s / %s / %s / %s", getString(R.string.transfer), getString(R.string.split_transaction), getString(R.string.income), getString(R.string.expense))
            }
        };

        open fun getLabelResId(ctx: Context) =
                ctx.resources.getIdentifier("discover_feature_$name", "string", ctx.packageName)

        fun toDescription(context: Context) = context.getString(getLabelResId(context))

        abstract fun toTitle(context: Context): String
    }

    override fun discover(context: Activity, target: View, daysSinceInstall: Int, feature: Feature,
                          measureTarget: Boolean) =
            (Utils.getDaysSinceInstall(context) >= daysSinceInstall && prefHandler.getBoolean(feature.key, true)).also {
                if (it) {
                    if (measureTarget) {
                        target.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                discoveryShow(context, target, feature, UiUtils.px2Dp(target.width / 2))
                                target.viewTreeObserver.removeGlobalOnLayoutListener(this)
                            }
                        })
                    } else {
                        discoveryShow(context, target, feature)
                    }
                }
            }

    override fun markDiscovered(feature: Feature) {
        prefHandler.putBoolean(feature.key, false)
        Timber.d("Marked as discoverd: %s", feature)
    }

    private fun discoveryShow(context: Activity, target: View, feature: Feature, targetRadius: Int? = null) {
        TapTargetView.showFor(context,
                TapTarget.forView(target, feature.toTitle(context), feature.toDescription(context))
                        .transparentTarget(true)
                        .outerCircleColorInt(UiUtils.getColor(context, R.attr.colorAccent))
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