package org.totschnig.myexpenses.util.ads;

import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.util.ads.customevent.AdListener;
import org.totschnig.myexpenses.util.ads.customevent.AdView;
import org.totschnig.myexpenses.util.ads.customevent.Interstitial;
import org.totschnig.myexpenses.util.ads.customevent.PartnerProgram;

import java.util.Arrays;

import androidx.core.util.Pair;

public class CustomAdHandler extends AdHandler {

  private AdView adView;
  private Interstitial interstitial;
  private boolean mInterstitialShown = false;
  private String userCountry;


  protected CustomAdHandler(AdHandlerFactory factory, ViewGroup adContainer, String userCountry) {
    super(factory, adContainer);
    this.userCountry = userCountry;
  }

  @Override
  public void _startBanner() {
    float density = context.getResources().getDisplayMetrics().density;
    Pair<PartnerProgram, String> contentProvider = PartnerProgram.pickContent(
        Arrays.asList(PartnerProgram.values()),
        userCountry,
        context,
        Math.round(adContainer.getWidth() / density));
    if (contentProvider == null) {
      hide();
    } else {
      adView = new AdView(context);
      adView.setAdListener(new AdListener() {
        @Override
        public void onBannerLoaded(View view) {
          adContainer.addView(view);
        }
      });
      adView.fetchAd(contentProvider);
    }
  }

  @Override
  protected boolean maybeShowInterstitialDo() {
    if (interstitial == null || mInterstitialShown) {
      return false;
    }
    interstitial.show();
    mInterstitialShown = true;
    return true;
  }

  @Override
  protected void requestNewInterstitialDo() {
    Pair<PartnerProgram, String> contentProvider = PartnerProgram.pickContent(Arrays.asList(PartnerProgram.values()),
        userCountry, context, -1);
    if (contentProvider != null) {
      mInterstitialShown = false;
      interstitial = new Interstitial(context);

      interstitial.setContentProvider(contentProvider);

      interstitial.setAdListener(new AdListener() {
      });

      interstitial.fetchAd();
    } else {
      onInterstitialFailed();
    }
  }

  @Override
  public void onDestroy() {
    if (adView != null) {
      adView.destroy();
    }
  }
}
