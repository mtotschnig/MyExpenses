package org.totschnig.myexpenses.testutils

import android.content.Context
import org.totschnig.myexpenses.di.FeatureModule
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider

object TestFeatureModule: FeatureModule() {
    override fun provideFeatureManager(localeProvider: UserLocaleProvider, prefHandler: PrefHandler) = TestFeatureManager
}

object TestFeatureManager: FeatureManager() {
    override fun isFeatureInstalled(feature: Feature, context: Context) = false
}

