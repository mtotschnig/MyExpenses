package org.totschnig.myexpenses.test.util

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.util.ShareUtils

@RunWith(AndroidJUnit4::class)
class ShareUtilsTest {
    @Test
    fun shouldConvertSingleFileUri() {
        val mimeType = "text/plain"
        val testFileUri = AppDirHelper.getAppDir(InstrumentationRegistry.getInstrumentation().targetContext)!!
            .createFile(mimeType, "testFile")!!.uri
        assertFileScheme(testFileUri)
        val fileUris = listOf(testFileUri)
        val intent = ShareUtils.buildIntent(fileUris, mimeType, null)
        val sharedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertContentScheme(sharedUri)
    }

    @Test
    fun shouldConvertMultipleFileUris() {
        val mimeType = "text/plain"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile1Uri = AppDirHelper.getAppDir(context)!!
            .createFile(mimeType, "testFile1")!!.uri
        val testFile2Uri = AppDirHelper.getAppDir(context)!!
            .createFile(mimeType, "testFile1")!!.uri
        val fileUris = listOf(testFile1Uri, testFile2Uri)
        fileUris.forEach { uri: Uri -> assertFileScheme(uri) }
        val intent = ShareUtils.buildIntent(fileUris, mimeType, null)
        val sharedUris: List<Uri> = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!
        sharedUris.forEach { uri: Uri? -> assertContentScheme(uri) }
    }

    private fun assertFileScheme(uri: Uri) {
        assertScheme(uri, "file")
    }

    private fun assertContentScheme(uri: Uri?) {
        assertScheme(uri, "content")
    }

    private fun assertScheme(uri: Uri?, scheme: String) {
        Assert.assertEquals(scheme, uri!!.scheme)
    }
}