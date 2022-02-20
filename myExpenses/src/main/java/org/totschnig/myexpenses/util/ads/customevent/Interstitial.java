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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

public class Interstitial implements View.OnClickListener {
  @Nullable private AdListener listener;
  private Pair<PartnerProgram, String> contentProvider;
  private final Context context;
  private final PrefHandler prefHandler;
  private Dialog mWebviewDialog;

  /**
   * Create a new {@link Interstitial}.
   *
   * @param context An Android {@link Context}.
   * @param prefHandler
   */
  public Interstitial(Context context, PrefHandler prefHandler) {
    this.context = context;
    this.prefHandler = prefHandler;
  }

  /**
   * Sets a {@link AdListener} to listen for ad events.
   *
   * @param listener The ad listener.
   */
  public void setAdListener(AdListener listener) {
    this.listener = listener;
  }

  /**
   * Fetch an ad. Currently does nothing, since content is stored locally
   */
  public void fetchAd() {
  }

  /**
   * Shows the interstitial.
   */
  public void show() {
    // Notify the developer that a full screen view will be presented.
    if (listener != null) {
      listener.onAdOpened();
    }
    openWebViewInOverlay();
    Bundle bundle = new Bundle(1);
    bundle.putString(Tracker.EVENT_PARAM_AD_PROVIDER, contentProvider.first.name());
    ((MyApplication) context.getApplicationContext()).getAppComponent().tracker().logEvent(Tracker.EVENT_AD_CUSTOM, bundle);
    contentProvider.first.record(prefHandler);
  }

  private void openWebViewInOverlay() {
    mWebviewDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

    View view = View.inflate(context, R.layout.webview_modal, null);
    View closeButton = view.findViewById(R.id.closeOverlay);
    closeButton.setOnClickListener(this);

    WebView webView = view.findViewById(R.id.webviewoverlay);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.loadData(String.format("<center>%s</center>", contentProvider.second), "text/html", "utf-8");
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (listener != null) {
          listener.onAdClicked();
          listener.onAdLeftApplication();
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(i);
        return true;
      }
    });
    mWebviewDialog.setContentView(view);
    mWebviewDialog.show();
  }


  @Override
  public void onClick(View v) {
    if (mWebviewDialog != null) {
      mWebviewDialog.dismiss();
      if (listener != null) {
        listener.onAdClosed();
      }
      destroy();
    }
  }

  /**
   * Destroy the interstitial.
   */
  private void destroy() {
    listener = null;
  }

  public void setContentProvider(Pair<PartnerProgram, String> contentProvider) {
    this.contentProvider = contentProvider;
  }
}

