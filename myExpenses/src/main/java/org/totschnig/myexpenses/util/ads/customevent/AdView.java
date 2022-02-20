/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.util.ads.customevent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;

import androidx.core.util.Pair;

public class AdView extends WebView {
  private AdListener listener;
  private PrefHandler prefHandler;

  /**
   * Create a new {@link AdView}.
   *
   * @param context An Android {@link Context}.
   * @param prefHandler
   */
  public AdView(Context context, PrefHandler prefHandler) {
    super(context);
    this.prefHandler = prefHandler;
    setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        listener.onAdClicked();
        listener.onAdOpened();
        listener.onAdLeftApplication();
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(i);
        return true;
      }
    });
  }

  /**
   * Sets a {@link AdListener} to listen for ad events.
   *
   * @param listener The ad listener.
   */
  public void setAdListener(AdListener listener) {
    this.listener = listener;
  }

  public void fetchAd(Pair<PartnerProgram, String> contentProvider) {
    if (listener == null) {
      return;
    }
    Bundle bundle = new Bundle(1);
    bundle.putString(Tracker.EVENT_PARAM_AD_PROVIDER, contentProvider.first.name());
    MyApplication.getInstance().getAppComponent().tracker().logEvent(Tracker.EVENT_AD_CUSTOM, bundle);
    this.loadData(String.format("<center>%s</center>", contentProvider.second), "text/html", "utf-8");
    listener.onBannerLoaded(this);
    contentProvider.first.record(prefHandler);
  }

  /**
   * Destroy the banner.
   */
  public void destroy() {
    listener = null;
  }
}
