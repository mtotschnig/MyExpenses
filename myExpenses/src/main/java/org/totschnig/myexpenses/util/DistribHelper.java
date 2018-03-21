package org.totschnig.myexpenses.util;

import android.content.Context;

import org.totschnig.myexpenses.BuildConfig;

import java.util.Locale;

public class DistribHelper {

  public enum Distribution {
    PLAY,
    AMAZON {
      @Override
      public String getMarketPrefix() {
        return "amzn://apps/android?p=";
      }
    },
    BLACKBERRY {
      @Override
      public String getPlattform() {
        return "Blackberry";
      }

      @Override
      public String getMarketSelfUri() {
        return "appworld://content/54472888";
      }
    },
    GITHUB;

    public String getPlattform() {
      return "Android";
    }

    public String getMarketPrefix() {
      return "market://details?id=";
    }

    public String getMarketSelfUri() {
      return getMarketPrefix() + "org.totschnig.myexpenses";
    }
  }

  public static String getPlatform() {
    return getDistribution().getPlattform();
  }

  public static String getMarketPrefix() {
    return getDistribution().getMarketPrefix();
  }

  public static String getMarketSelfUri() {
    return getDistribution().getMarketSelfUri();
  }

  /**
   *
   * @return if we should use the Platform calendar for managing instances. Events are always stored
   * in the platform calendar
   */
  public static boolean shouldUseAndroidPlatformCalendar() {
    return !isBlackberry();
  }

  public static Distribution getDistribution() {
    return Distribution.valueOf(BuildConfig.DISTRIBUTION);
  }

  public static String getDistributionAsString() {
    return getDistribution().toString();
  }

  public static boolean isBlackberry() {
    return getDistribution().equals(Distribution.BLACKBERRY);
  }

  public static boolean isPlay() {
    return getDistribution().equals(Distribution.PLAY);
  }

  public static boolean isAmazon() {
    return getDistribution().equals(Distribution.AMAZON);
  }

  public static boolean isGithub() {
    return getDistribution().equals(Distribution.GITHUB);
  }

  /**
   * retrieve information about the current version
   *
   * @return concatenation of versionName, versionCode and buildTime
   * buildTime is automatically stored in property file during build process
   */
  public static String getVersionInfo(Context ctx) {
    String version = "(revision " + getVersionNumber() + ")";
    String versionname = BuildConfig.VERSION_NAME;
    String buildDate = BuildConfig.BUILD_DATE;

    final String flavor = getDistributionAsString();
    String installer = ctx.getPackageManager()
        .getInstallerPackageName(ctx.getPackageName());
    return String.format(Locale.ROOT, "%s %s %s %s %s",
        versionname, version, buildDate, flavor, installer);
  }

  /**
   * @return version number (versionCode)
   */
  public static int getVersionNumber() {
    return BuildConfig.VERSION_CODE;
  }
}
