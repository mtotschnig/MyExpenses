package org.totschnig.myexpenses.util.ads.customevent;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;

public enum PartnerProgram {
  ;

  private static final String CONTENT_RES_PREFIX = "custom_ads_html_";
  private static final String PREFERENCE_PREFIX = "custom_ads_last_shown_";

  private enum MyAdSize {
    SMALL(200,50),
    BANNER(320, 50),
    FULL_BANNER(468, 60),
    LEADERBOARD(728, 90);

    private final int width;
    private final int height;

    MyAdSize(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }
  }

  private final List<String> distributionCountries;
  /**
   * List should be sorted by width then height, since this order is expected by {@link #pickContentResId(Context, int)}
   */
  private final List<MyAdSize> adSizes;

  PartnerProgram(String[] distributionCountries, MyAdSize[] adSizes) {
    this.distributionCountries = new ArrayList<>(Arrays.asList(distributionCountries));
    this.adSizes = new ArrayList<>(Arrays.asList(adSizes));
  }

  public boolean shouldShowIn(String country) {
    return distributionCountries.contains(country) &&
        System.currentTimeMillis() - MyApplication.getInstance().getSettings().getLong(getPrefKey(),0) > HOUR_IN_MILLIS * 4;
  }

  @NonNull
  private String getPrefKey() {
    return PREFERENCE_PREFIX + name();
  }

  @ArrayRes
  public int pickContentResId(Context context, int availableWidth) {
    return Stream.of(adSizes).filter(value -> availableWidth >= value.getWidth())
        .peek(myAdSize -> Timber.d("%s", myAdSize))
        .map(adSize -> {
          final String name = CONTENT_RES_PREFIX + name() + "_" + adSize.name();
          Timber.d(name);
          return context.getResources().getIdentifier(name, "array", context.getPackageName());
        })
        .filter(resId -> resId != 0)
        .findFirst().orElse(0);
  }

  @ArrayRes
  public int pickContentInterstitial(Context context) {
    int orientation = context.getResources().getConfiguration().orientation;
    final String name = CONTENT_RES_PREFIX + name() + "_" + (orientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE");
    return context.getResources().getIdentifier(name, "array", context.getPackageName());
  }

  public void record() {
    MyApplication.getInstance().getSettings().edit().putLong(getPrefKey(), System.currentTimeMillis()).apply();
  }

  @Nullable
  public static Pair<PartnerProgram, String> pickContent(
      List<PartnerProgram> partnerPrograms, String userCountry, Context context,
      int availableWidth) {
    boolean forInterstitial = availableWidth == -1;
    List<Pair<PartnerProgram, Integer>> contentProviders = Stream.of(partnerPrograms)
        .filter(partnerProgram -> partnerProgram.shouldShowIn(userCountry))
        .map(partnerProgram ->
            Pair.create(partnerProgram, forInterstitial ? partnerProgram.pickContentInterstitial(context) :
                partnerProgram.pickContentResId(context, availableWidth)))
        .filter(pair -> pair.second != 0)
        .collect(Collectors.toList());
    final int nrOfProviders = contentProviders.size();
    if (nrOfProviders > 0) {
      final Random random = new Random();
      Pair<PartnerProgram, Integer> contentProvider;
      if (nrOfProviders == 1) {
        contentProvider = contentProviders.get(0);
      } else {
        contentProvider = contentProviders.get(random.nextInt(nrOfProviders));
      }
      String[] adContent = context.getResources().getStringArray(contentProvider.second);
      return Pair.create(contentProvider.first, adContent[random.nextInt(adContent.length)]);
    }
    return null;
  }
}
