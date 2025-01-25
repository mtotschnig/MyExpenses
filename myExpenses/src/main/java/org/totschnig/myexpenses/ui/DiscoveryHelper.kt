package org.totschnig.myexpenses.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ui.UiUtils
import timber.log.Timber

class DiscoveryHelper(val prefHandler: PrefHandler) : IDiscoveryHelper {

    enum class Feature(val key: String) {
        ExpenseIncomeSwitch("showDiscoveryExpenseIncomeSwitch") {
            override fun toTitle(context: Context) = with(context) {
                String.format("%s / %s", getString(R.string.expense), getString(R.string.income))
            }
        },
        FabLongPress("showDiscoveryFabLongPress") {
            override fun toTitle(context: Context) = with(context) {
                String.format(
                    "%s / %s / %s / %s",
                    getString(R.string.transfer),
                    getString(R.string.split_transaction),
                    getString(R.string.income),
                    getString(R.string.expense)
                )
            }
        };

        open fun getLabelResId(ctx: Context) = when (this) {
            ExpenseIncomeSwitch -> R.string.discover_feature_expense_income_switch
            FabLongPress -> R.string.discover_feature_fab_long_press
        }

        fun toDescription(context: Context) = context.getString(getLabelResId(context))

        abstract fun toTitle(context: Context): String
    }

    override fun discover(
        context: Activity, target: View, daysSinceInstall: Int, feature: Feature,
        measureTarget: Boolean,
    ) =
        (Utils.getDaysSinceInstall(context) >= daysSinceInstall && prefHandler.getBoolean(
            feature.key,
            true
        )).also {
            if (it) {
                if (measureTarget) {
                    target.viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            discoveryShow(context, target, feature, UiUtils.px2Dp(target.width / 2))
                            target.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    })
                } else {
                    discoveryShow(context, target, feature)
                }
            }
        }

    override fun markDiscovered(feature: Feature) {
        prefHandler.putBoolean(feature.key, false)
        Timber.d("Marked as discovered: %s", feature)
    }

    private fun discoveryShow(
        context: Activity,
        target: View,
        feature: Feature,
        targetRadius: Int? = null,
    ) {
        val title = feature.toTitle(context)
        val description = feature.toDescription(context)
        val tapTargetView = TapTargetView.showFor(context,
            TapTarget.forView(target, title, description)
                .transparentTarget(true)
                .outerCircleColorInt(
                    UiUtils.getColor(
                        context,
                        androidx.appcompat.R.attr.colorAccent
                    )
                )
                .apply {
                    targetRadius?.let { this.targetRadius(it) }
                },
            object : TapTargetView.Listener() {
                override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                    markDiscovered(feature)
                }
            })
        ViewCompat.setAccessibilityDelegate(tapTargetView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = "$title: $description"
                info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS)
                info.isDismissable = true
            }

            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: Bundle?,
            ) = if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS) {
                tapTargetView.dismiss(false)
                true
            } else super.performAccessibilityAction(host, action, args)
        })
    }
}