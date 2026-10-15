package org.totschnig.myexpenses.test.misc

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.injector

@RunWith(AndroidJUnit4::class)
class SafeGuardTest {

    @Test
    fun contribIsNotEnabled() {
        val licenceHandler = InstrumentationRegistry.getInstrumentation().targetContext.injector.licenceHandler()
        assertThat(licenceHandler.isContribEnabled).isFalse()
        assertThat(licenceHandler.licenceStatus).isNull()
    }
}
