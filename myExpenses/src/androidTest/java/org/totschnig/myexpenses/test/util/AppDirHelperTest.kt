package org.totschnig.myexpenses.test.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.util.AppDirHelper.checkAppDir

@RunWith(AndroidJUnit4::class)
class AppDirHelperTest {
    @Test
    fun shouldValidateDefaultAppDir() {
        Assert.assertTrue(checkAppDir(InstrumentationRegistry.getInstrumentation().targetContext).isSuccess)
    }
}