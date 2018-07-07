package org.totschnig.myexpenses.util.ads;

import android.support.v4.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.ads.customevent.AdListener;
import org.totschnig.myexpenses.util.ads.customevent.AdView;
import org.totschnig.myexpenses.util.ads.customevent.Interstitial;
import org.totschnig.myexpenses.util.ads.customevent.PartnerProgram;

import java.util.Arrays;

public class CustomAdHandler extends AdHandler {

  private AdView adView;
  private Interstitial interstitial;

  protected CustomAdHandler(AdHandlerFactory factory, ViewGroup adContainer) {
    super(factory, adContainer);
  }

  @Override
  public void init() {
    if (shouldHideAd()) {
      hide();
    } else {
      float density = context.getResources().getDisplayMetrics().density;
      Pair<PartnerProgram, String> contentProvider = PartnerProgram.pickContent(
          Arrays.asList(PartnerProgram.values()),
          MyApplication.getInstance().getAppComponent().userCountry(),
          context,
          Math.round(adContainer.getWidth() / density));
      if (contentProvider == null) {
        hide();
      } else {
        adView = new AdView(context);
        // Implement a SampleAdListener and forward callbacks to mediation. The callback forwarding
        // is handled by SampleBannerEventForwarder.
        adView.setAdListener(new AdListener() {
          @Override
          public void onBannerLoaded(View view) {
            adContainer.addView(view);
          }
        });
        adView.fetchAd(contentProvider);
      }
    }
  }


  @Override
  synchronized protected boolean maybeShowInterstitialDo() {
    if (interstitial != null) {
      interstitial.show();
      return true;
    }
    return false;
  }

  @Override
  synchronized protected void requestNewInterstitialDo() {
    Pair<PartnerProgram, String> contentProvider = PartnerProgram.pickContent(Arrays.asList(PartnerProgram.values()),
        MyApplication.getInstance().getAppComponent().userCountry(), context, -1);
    if (contentProvider != null) {
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
