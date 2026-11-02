package org.totschnig.myexpenses.next.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * The settings of one area of the new UI in [NextSettings], e.g. the rules of categories, built by
 * its [Factory] for the activity. The areas are listed in [org.totschnig.myexpenses.next.NextFeatures.settings].
 */
interface AreaSettings {
    /** The control of [setting], null if it is not one of the area */
    @Composable
    fun control(setting: InsightSetting): SettingControl?

    /** The page a [SettingControl.Page] of the area opens */
    @Composable
    fun Page(setting: InsightSetting, onBack: () -> Unit) {
        LaunchedEffect(setting) { onBack() }
    }

    /** Dialogs over the settings, e.g. to confirm a reset */
    @Composable
    fun Dialogs() {}

    interface Factory {
        /** The settings of the area, also without an activity, e.g. for the search */
        val settings: List<InsightSetting>

        fun create(activity: ComponentActivity): AreaSettings
    }
}
