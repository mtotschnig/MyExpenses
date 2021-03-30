package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Test;
import org.mockito.Mockito;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DistributionHelperTest {

  @Test
  public void distributionShouldBeTakenUpFromBuildConfig() {
    assertEquals(DistributionHelper.getDistributionAsString(), BuildConfig.DISTRIBUTION);
  }

  @Test
  public void shouldReportVersionCodeFromBuildConfig() {
    assertEquals(DistributionHelper.getVersionNumber(), BuildConfig.VERSION_CODE);
  }

  @Test
  public void shouldFormatVersionInfoWithInstaller() {
    //given
    String playInstaller = "com.android.vending";
    Context mockContext = mockContextWithInstaller(playInstaller);
    //when
    String versionInfo = DistributionHelper.getVersionInfo(mockContext);
    //then
    String expected = String.format(Locale.ROOT, "%s (revision %d) %s %s %s",
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.BUILD_DATE,
        BuildConfig.DISTRIBUTION, playInstaller);
    assertEquals(expected, versionInfo);
  }

  @Test
  public void shouldFormatVersionInfoWithoutInstaller() {
    //given
    Context mockContext = mockContextWithInstaller(null);
    //when
    String versionInfo = DistributionHelper.getVersionInfo(mockContext);
    //then
    String expected = String.format(Locale.ROOT, "%s (revision %d) %s %s null",
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.BUILD_DATE,
        BuildConfig.DISTRIBUTION);
    assertEquals(expected, versionInfo);
  }

  private Context mockContextWithInstaller(String installer) {
    Context mockContext = Mockito.mock(Context.class);
    PackageManager mockPackageManager = Mockito.mock(PackageManager.class);
    when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
    when(mockContext.getPackageName()).thenReturn(BuildConfig.APPLICATION_ID);

    when(mockPackageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID)).thenReturn(installer);
    return mockContext;
  }
}
