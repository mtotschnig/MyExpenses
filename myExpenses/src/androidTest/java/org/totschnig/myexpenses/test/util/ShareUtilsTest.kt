package org.totschnig.myexpenses.test.util

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.util.AppDirHelper
import org.totschnig.myexpenses.viewmodel.BaseFunctionalityViewModel

@RunWith(AndroidJUnit4::class)
class ShareUtilsTest {
    @Test
    fun shouldConvertSingleFileUri() {
        val mimeType = "text/plain"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appDir = AppDirHelper.getAppDir(context).getOrThrow()
        val testFile = appDir.createFile(mimeType, "testFile")!!
        val testFileUri = testFile.uri
        assertFileScheme(testFileUri)
        val fileUris = listOf(testFileUri)
        val intent = BaseFunctionalityViewModel.buildIntent(context, fileUris, mimeType, null)
        val sharedUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)!!
        assertContentScheme(sharedUris.first())
        //cleanup
        testFile.delete()
    }

    @Test
    fun shouldConvertMultipleFileUris() {
        val mimeType = "text/plain"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appDir = AppDirHelper.getAppDir(context).getOrThrow()
        val testFile1 = appDir.createFile(mimeType, "testFile1")!!
        val testFile2 = appDir.createFile(mimeType, "testFile2")!!
        val testFile1Uri = testFile1.uri
        val testFile2Uri = testFile2.uri
        val fileUris = listOf(testFile1Uri, testFile2Uri)
        fileUris.forEach { uri: Uri -> assertFileScheme(uri) }
        val intent = BaseFunctionalityViewModel.buildIntent(context, fileUris, mimeType, null)
        val sharedUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)!!
        sharedUris.forEach { uri: Uri -> assertContentScheme(uri) }
        //cleanup
        testFile1.delete()
        testFile2.delete()
    }

    private fun assertFileScheme(uri: Uri) {
        assertScheme(uri, "file")
    }

    private fun assertContentScheme(uri: Uri) {
        assertScheme(uri, "content")
    }

    private fun assertScheme(uri: Uri, scheme: String) {
        Truth.assertThat(uri.scheme).isEqualTo(scheme)
    }
}