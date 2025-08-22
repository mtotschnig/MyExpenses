package org.totschnig.myexpenses.util

import android.content.Context
import android.content.pm.PackageManager
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.util.distrib.DistributionHelper.buildDateFormatted
import org.totschnig.myexpenses.util.distrib.DistributionHelper.distributionAsString
import org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo
import org.totschnig.myexpenses.util.distrib.DistributionHelper.versionNumber
import java.util.*

class DistributionHelperTest {
    @Test
    fun distributionShouldBeTakenUpFromBuildConfig() {
        Assert.assertEquals(distributionAsString, BuildConfig.DISTRIBUTION)
    }

    @Test
    fun shouldReportVersionCodeFromBuildConfig() {
        Assert.assertEquals(versionNumber.toLong(), BuildConfig.VERSION_CODE.toLong())
    }

    @Test
    fun shouldFormatVersionInfoWithInstaller() {
        //given
        val playInstaller = "com.android.vending"
        val mockContext = mockContextWithInstaller(playInstaller)
        //when
        val versionInfo = getVersionInfo(mockContext)
        //then
        val expected = String.format(
            Locale.ROOT, "%s (%d) %s %s %s",
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, buildDateFormatted,
            BuildConfig.DISTRIBUTION, playInstaller
        )
        Assert.assertEquals(expected, versionInfo)
    }

    @Test
    fun shouldFormatVersionInfoWithoutInstaller() {
        //given
        val mockContext = mockContextWithInstaller(null)
        //when
        val versionInfo = getVersionInfo(mockContext)
        //then
        val expected = String.format(
            Locale.ROOT, "%s (%d) %s %s null",
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, buildDateFormatted,
            BuildConfig.DISTRIBUTION
        )
        Assert.assertEquals(expected, versionInfo)
    }

    private fun mockContextWithInstaller(installer: String?): Context {
        val mockContext = Mockito.mock(Context::class.java)
        val mockPackageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(mockContext.packageManager).thenReturn(mockPackageManager)
        Mockito.`when`(mockContext.packageName).thenReturn(BuildConfig.APPLICATION_ID)
        Mockito.`when`(mockPackageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID))
            .thenReturn(installer)
        return mockContext
    }
}