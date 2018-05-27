package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.pubnative.sdk.core.request.PNAdModel;
import net.pubnative.sdk.core.request.PNRequest;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;

import timber.log.Timber;

public class PubNativeAdHandler extends AdHandler {
  private static final String APP_TOKEN = "d7757800d02945a18bbae190a9a7d4d1";
  private static final String PLACEMENT_NAME = BuildConfig.DEBUG ? "Test" : "Banner";
  private static final String PROVIDER = "PubNative";
  private final Context context;
  private TextView title;
  private TextView description;
  private ImageView icon;
  private ViewGroup adRoot;
  private Button install;
  private RelativeLayout banner;
  private ViewGroup disclosure;

  public PubNativeAdHandler(ViewGroup adContainer) {
    super(adContainer);
    this.context = adContainer.getContext();
  }

  @Override
  public void init() {
    if (isAdDisabled()) {
      hide();
    } else {
      PNRequest request = new PNRequest();
      adRoot = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pubnative_my_banner, adContainer, false);

      banner = adRoot.findViewById(R.id.pubnative_banner_view);
      title = adRoot.findViewById(R.id.pubnative_banner_title);
      description = adRoot.findViewById(R.id.pubnative_banner_description);
      icon = adRoot.findViewById(R.id.pubnative_banner_image);
      install = adRoot.findViewById(R.id.pubnative_banner_button);
      disclosure = adRoot.findViewById(R.id.ad_disclosure);

      adContainer.addView(adRoot);
      trackBannerRequest(PROVIDER);
      request.start(context, APP_TOKEN, PLACEMENT_NAME, new PNRequest.Listener() {

        @Override
        public void onPNRequestLoadFinish(PNRequest request, PNAdModel model) {
          trackBannerLoaded(PROVIDER);
          banner.setVisibility(View.VISIBLE);
          model.withTitle(title)
              .withDescription(description)
              .withContentInfoContainer(disclosure)
              .withIcon(icon)
              .withCallToAction(install)
              .startTracking(adRoot);
        }

        @Override
        public void onPNRequestLoadFail(PNRequest request, Exception exception) {
          trackBannerFailed(PROVIDER, exception.getMessage());
          hide();
        }
      });
    }
  }

  @Override
  protected boolean maybeShowInterstitialDo() {
    return false;
  }

  @Override
  protected void requestNewInterstitialDo() {
    onInterstitialFailed();
  }
}
