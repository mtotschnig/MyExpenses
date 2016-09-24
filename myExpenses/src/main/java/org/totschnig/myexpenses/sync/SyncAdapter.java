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
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.util.ArrayList;
import java.util.List;

class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final String TAG = "SyncAdapter";
  private final ContentResolver mContentResolver;

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    mContentResolver = context.getContentResolver();
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
    super(context, autoInitialize, allowParallelSyncs);
    mContentResolver = context.getContentResolver();
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider, SyncResult syncResult) {
    Log.i(TAG, "onPerformSync");
    String sequence = "0"; //TODO obtain current sequence from db and as a side effect increase sequence
                           //visible for the triggers
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
    List<TransactionChange> localChanges = new ArrayList<>();
    try {
      Cursor c = provider.query(TransactionProvider.CHANGES_URI.buildUpon()
              .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, account.name.substring(1))
              .appendQueryParameter(DatabaseConstants.KEY_SYNC_SEQUENCE, sequence)
              .build(),
          null, null, null, null);
      if (c != null) {
        if (c.moveToFirst()) {
          do {
            localChanges.add(TransactionChange.create(c));
          } while (c.moveToNext());
        }
        c.close();
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    for (TransactionChange change : localChanges) {
      Log.i(TAG, gson.toJson(change));
    }
    //get remote changes since sequence
    //get local changes
    //merge
    //filter out afterDelete changes
    //sort
    //iterate
    //write local change to remote source
    //write remote change to db
    //store new sequence
  }
}
