package org.totschnig.myexpenses.ui

import android.app.Activity
import android.content.Context
import android.view.View
import org.totschnig.myexpenses.R

interface IDiscoveryHelper {
    fun discover(context: Activity, target: View, daysSinceInstall: Int, feature: Feature,
                 measureTarget: Boolean = false): Boolean

    fun markDiscovered(feature: Feature)

    companion object NO_OP : IDiscoveryHelper {
        override fun discover(context: Activity, target: View, daysSinceInstall: Int, feature: Feature, measureTarget: Boolean) = false

        override fun markDiscovered(feature: Feature) {}
    }

    enum class Feature(val key: String) {
        ExpenseIncomeSwitch("showDiscoveryExpenseIncomeSwitch") {
            override fun toTitle(context: Context) = with(context) {
                String.format("%s / %s", getString(R.string.expense), getString(R.string.income))
            }
        };

        open fun getLabelResId(ctx: Context) = when (this) {
            ExpenseIncomeSwitch -> R.string.discover_feature_expense_income_switch
        }

        fun toDescription(context: Context) = context.getString(getLabelResId(context))

        abstract fun toTitle(context: Context): String
    }
}