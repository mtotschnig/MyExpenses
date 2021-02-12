package org.totschnig.myexpenses.ui

import android.app.Activity
import android.view.View

interface IDiscoveryHelper {
    fun discover(context: Activity, target: View, daysSinceInstall: Int, feature: DiscoveryHelper.Feature,
                 measureTarget: Boolean = false): Boolean

    fun markDiscovered(feature: DiscoveryHelper.Feature)

    companion object NO_OP : IDiscoveryHelper {
        override fun discover(context: Activity, target: View, daysSinceInstall: Int, feature: DiscoveryHelper.Feature, measureTarget: Boolean) = false

        override fun markDiscovered(feature: DiscoveryHelper.Feature) {}
    }
}