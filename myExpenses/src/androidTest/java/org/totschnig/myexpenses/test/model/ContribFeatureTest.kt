package org.totschnig.myexpenses.test.model

import com.google.common.truth.Truth.assertThat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.testutils.MockLicenceHandler
import org.totschnig.myexpenses.model.ContribFeature

class ContribFeatureTest : ModelTest() {
    fun testRecordUsage() {
        val feature = ContribFeature.DISTRIBUTION
        val appComponent = (context.applicationContext as MyApplication).appComponent
        val licenceHandler = appComponent.licenceHandler() as MockLicenceHandler
        val prefHandler = appComponent.prefHandler()
        var expectedCount = ContribFeature.USAGES_LIMIT
        assertThat(feature.usagesLeft(prefHandler)).isEqualTo(expectedCount)
        licenceHandler.setLockState(true)
        feature.recordUsage(prefHandler, licenceHandler)
        expectedCount--
        assertThat(feature.usagesLeft(prefHandler)).isEqualTo(expectedCount)
        licenceHandler.setLockState(false)
        feature.recordUsage(prefHandler, licenceHandler)
        assertThat(feature.usagesLeft(prefHandler)).isEqualTo(expectedCount)
        licenceHandler.setLockState(true)
        feature.recordUsage(prefHandler, licenceHandler)
        expectedCount--
        assertThat(feature.usagesLeft(prefHandler)).isEqualTo(expectedCount)
    }
}