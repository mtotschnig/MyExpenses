package org.totschnig.myexpenses.testutils

import android.content.Context
import android.net.Uri
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.di.FeatureModule
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.preference.PrefHandler

object TestFeatureModule : FeatureModule() {
    override fun provideFeatureManager(prefHandler: PrefHandler) = TestFeatureManager

    var testOcrFeature: OcrFeature = object : OcrFeature {
        override fun startCapture(
            activity: BaseActivity,
            fileName: String,
            onResult: (Uri) -> Unit,
            onCancel: (() -> Unit)?
        ) {
            onResult(Uri.parse("file:///android_asset/screenshot.jpg"))
        }
    }

    override fun provideOcrFeature(prefHandler: PrefHandler): OcrFeature = testOcrFeature
}

object TestFeatureManager : FeatureManager() {
    // we assume running with sqlcrypt installed, provideSQLiteOpenHelper in TestDataModule mocks an
    // encrypted database just by virtue of the file name "data.enc"
    override fun isFeatureInstalled(feature: Feature, context: Context) =
        feature == Feature.SQLCRYPT || feature == Feature.CAPTURE || feature == Feature.OCR
}
