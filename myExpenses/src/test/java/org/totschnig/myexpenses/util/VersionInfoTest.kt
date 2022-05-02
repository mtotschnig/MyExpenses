package org.totschnig.myexpenses.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.data.VersionInfo

@RunWith(RobolectricTestRunner::class)
class VersionInfoTest {
    @Test
    fun shouldProvideVersionInfoForCurrentVersion() {
        Assume.assumeFalse(BuildConfig.BETA)
        if (BuildConfig.VERSION_NAME.length == 5) { // bug fix version do not need more_info
            val resIdMoreInfo = resId("version_more_info_")
            Truth.assertThat(resIdMoreInfo).isNotEqualTo(0)
        }
        val resIdGithubBoard = resId("project_board_")
        Truth.assertThat(resIdGithubBoard).isNotEqualTo(0)
    }

    @Test
    fun testVersionCodes() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val versions = context.resources.getStringArray(R.array.versions)
        versions.forEach {
            val parts = it.split(';')
            Truth.assertThat(parts.size).isEqualTo(2)
            Truth.assertThat(VersionInfo(parts[0].toInt(), parts[1]).getChanges(context)).isNotNull()
        }
    }

    private fun resId(prefix: String) = with(ApplicationProvider.getApplicationContext<Application>()) {
        resources.getIdentifier(
            "$prefix${BuildConfig.VERSION_NAME.replace(".", "")}",
            "string",
            packageName)
    }
}