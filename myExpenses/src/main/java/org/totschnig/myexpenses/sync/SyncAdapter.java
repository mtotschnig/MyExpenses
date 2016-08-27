package org.totschnig.myexpenses.sync;

/*
 * Copyright 2013 The Android Open Source Project
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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final String TAG = "SyncAdapter";
  private final ContentResolver mContentResolver;

  /**
   * Constructor. Obtains handle to content resolver for later use.
   */
  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    mContentResolver = context.getContentResolver();
  }

  /**
   * Constructor. Obtains handle to content resolver for later use.
   */
  public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
    super(context, autoInitialize, allowParallelSyncs);
    mContentResolver = context.getContentResolver();
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider, SyncResult syncResult) {
    Log.i(TAG, "TODO");
  }
}
