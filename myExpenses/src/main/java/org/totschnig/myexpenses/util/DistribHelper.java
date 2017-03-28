package org.totschnig.myexpenses.util;

import org.totschnig.myexpenses.BuildConfig;

public class DistribHelper {

  private enum Distribution {
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

  public static boolean shouldUseAndroidPlatformCalendar() {
    return !isBlackberry();
  }

  private static Distribution getDistribution() {
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
}
