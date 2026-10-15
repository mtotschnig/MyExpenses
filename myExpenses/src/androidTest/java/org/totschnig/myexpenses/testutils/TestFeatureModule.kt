package org.totschnig.myexpenses.testutils

import android.content.Context
import org.totschnig.myexpenses.di.FeatureModule
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.PrefHandler

object TestFeatureModule: FeatureModule() {
    override fun provideFeatureManager(prefHandler: PrefHandler) = TestFeatureManager
}

object TestFeatureManager: FeatureManager() {
    override fun isFeatureInstalled(feature: Feature, context: Context) =
        feature == Feature.SQLCRYPT // we assume running with sqlcrypt installed, provideSQLiteOpenHelper in TestDataModule mocks an encrypted database just by virtue of the file name "data.enc"
}

