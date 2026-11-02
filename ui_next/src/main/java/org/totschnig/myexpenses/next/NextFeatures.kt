package org.totschnig.myexpenses.next

import androidx.compose.runtime.ProvidedValue
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.next.settings.AreaSettings
import org.totschnig.myexpenses.next.settings.NextSettings

/**
 * The features of the new UI, the only place that lists them. A feature needs the features it is
 * given here, so they come first; [all] is the order in which the shell shows their parts, e.g. the
 * sections of the details and the tabs. Each area adds its entry here, see [NextFeature].
 */
class NextFeatures(private val activity: BaseActivity) {

    val all: List<NextFeature> = listOf()

    companion object {
        /** What the areas provide to all screens of the new UI, also outside of the main screen, see [NextTheme] */
        val locals: Array<ProvidedValue<*>> get() = arrayOf()

        /** The settings of the areas, in the order they are listed in their groups, see [NextSettings] */
        val settings: List<AreaSettings.Factory> get() = listOf()
    }
}
