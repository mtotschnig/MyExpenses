package org.totschnig.myexpenses.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.data.VersionInfo

@RunWith(RobolectricTestRunner::class)
class VersionInfoTest {

    val context
        get() = ApplicationProvider.getApplicationContext<Application>()

    val currentVersion
     get()= VersionInfo(context.resources.getStringArray(R.array.versions).first())

    val isBugFixVersion
        get() = currentVersion.name.split('.').size == 4

    @Test
    fun currentVersionFromBuildConfigMatches() {
        assertThat(BuildConfig.VERSION_NAME).isEqualTo(currentVersion.name)
    }

    @Test
    fun testVersionLinksForCurrentVersion() {
        Assume.assumeFalse(BuildConfig.BETA)
        val context = ApplicationProvider.getApplicationContext<Application>()
        urlReachable(currentVersion.githubUrl(context)!!)
        if (!isBugFixVersion) {
            urlReachable(currentVersion.mastodonUrl(context)!!)
        }
    }

    @Test
    fun testVersionCodes() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val versions = context.resources.getStringArray(R.array.versions)
        versions.forEach {
            assertWithMessage("No changes for version $it").that(VersionInfo(it).getChanges(context)).isNotNull()
        }
    }

    private fun urlReachable(url:String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        val response = client.newCall(request).execute()

        Truth.assertWithMessage("Error retrieving $url").that(response.code).isEqualTo(200)
    }

}