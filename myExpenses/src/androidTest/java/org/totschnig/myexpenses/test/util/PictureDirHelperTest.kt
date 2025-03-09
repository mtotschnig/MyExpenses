package org.totschnig.myexpenses.test.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.TestApp
import org.totschnig.myexpenses.util.AppDirHelper.getContentUriForFile
import org.totschnig.myexpenses.util.PictureDirHelper.getFileForUri
import org.totschnig.myexpenses.util.PictureDirHelper.getOutputMediaUri

@RunWith(AndroidJUnit4::class)
class PictureDirHelperTest {
    val app: TestApp
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApp

    @Test
    fun unProtectedPictureIsBuildAndHasUriScheme() {
        Assert.assertEquals("content", getOutputMediaUri(false, app).scheme)
    }

    @Test
    fun protectedPictureIsBuildAndHasUriScheme() {
        Assert.assertEquals("content", getOutputMediaUri(false, app).scheme)
    }

    @Test
    fun fileForUnProtectedUriIsRetrievedAndMapsToOriginalUri() {
            //given
            val uri = getOutputMediaUri(false, app)
            //then
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val file = getFileForUri(targetContext, uri)
            Assert.assertEquals(getContentUriForFile(targetContext, file), uri)
        }

    @Test
    fun fileForProtectedUriIsRetrievedAndMapsToOriginalUri() {
            //given
            val uri = getOutputMediaUri(false, app)
            //then
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val file = getFileForUri(targetContext, uri)
            Assert.assertEquals(getContentUriForFile(targetContext, file), uri)
        }
}