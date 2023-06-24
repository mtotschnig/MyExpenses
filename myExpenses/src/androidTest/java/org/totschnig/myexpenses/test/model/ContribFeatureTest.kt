package org.totschnig.myexpenses.test.model

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.testutils.MockLicenceHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import java.time.Duration

class ContribFeatureTest {

    @Test
    fun testRecordUsage() {
        val feature = ContribFeature.DISTRIBUTION
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApp
        val licenceHandler = application.licenceHandler as MockLicenceHandler
        assertThat(licenceHandler.usagesLeft(feature)).isTrue()
        application.advanceClock(Duration.ofDays(LicenceHandler.TRIAL_DURATION_DAYS))
        assertThat(licenceHandler.usagesLeft(feature)).isTrue()
        licenceHandler.recordUsage(feature)
        assertThat(licenceHandler.usagesLeft(feature)).isTrue()
        application.advanceClock(Duration.ofDays(LicenceHandler.TRIAL_DURATION_DAYS))
        assertThat(licenceHandler.usagesLeft(feature)).isFalse()
    }
}