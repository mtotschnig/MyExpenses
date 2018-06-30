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

import android.view.View;

/**
 * A sample ad listener to listen for ad events. These ad events more or less represent the events
 * that a typical ad network would provide.
 */
public abstract class AdListener {

  public void onAdFailedToLoad(int errorCode) {}

  public void onAdOpened() {}


  public void onAdClicked() {}


  public void onAdClosed() {}


  public void onAdLeftApplication() {}


  public void onBannerLoaded(View view) {}


  public void onInterstitialLoaded() { }
}
