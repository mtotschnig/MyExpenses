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
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.ACCOUNT_SERVICE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;

class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final String TAG = "SyncAdapter";

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
    super(context, autoInitialize, allowParallelSyncs);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider, SyncResult syncResult) {
    Log.i(TAG, "onPerformSync");
    AccountManager accountManager = (AccountManager) getContext().getSystemService(ACCOUNT_SERVICE);
    String current_sync = accountManager.getUserData(account, KEY_SYNC_SEQUENCE_LOCAL);
    String accountId = account.name.substring(1);
    SyncBackend backend = getBackendForAccount(accountId);
    if (backend == null) {
      //TODO report
      return;
    }
    if (current_sync == null) {
      current_sync = "0";
    }

    List<TransactionChange> localChanges = new ArrayList<>();
    try {
      Uri changesUri = buildChangesUri(current_sync, accountId);
      if (hasLocalChanges(provider, changesUri)) {
        int nextSync = Integer.parseInt(current_sync) + 1;
        ContentValues currentSyncIncrease = new ContentValues(1);
        currentSyncIncrease.put(KEY_SYNC_SEQUENCE_LOCAL, nextSync);
        provider.update(TransactionProvider.ACCOUNTS_URI, currentSyncIncrease, KEY_ROWID + " = ?",
            new String[]{accountId});
        Cursor c = provider.query(changesUri, null, null, null, null);
        if (c != null) {
          if (c.moveToFirst()) {
            do {
              localChanges.add(TransactionChange.create(c));
            } while (c.moveToNext());
          }
          c.close();
        }
        if (localChanges.size() > 0) {
          backend.lock();
          try {
            backend.writeChangeSet(localChanges);
          } catch (IOException e) {
            e.printStackTrace();
          }
          backend.unlock();
          accountManager.setUserData(account, KEY_SYNC_SEQUENCE_LOCAL, String.valueOf(nextSync));
        }
      }
    } catch (RemoteException e) {
      e.printStackTrace();
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

  protected Uri buildChangesUri(String current_sync, String accountId) {
    return TransactionProvider.CHANGES_URI.buildUpon()
            .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, accountId)
            .appendQueryParameter(KEY_SYNC_SEQUENCE_LOCAL, current_sync)
            .build();
  }

  private SyncBackend getBackendForAccount(String accountId) {
    File baseFolder = new File("/sdcard/Debug");
    if (!baseFolder.isDirectory()) {
      return null;
    }
    File accountFolder = new File(baseFolder, "_" + accountId);
    accountFolder.mkdir();
    if (!accountFolder.isDirectory()) {
      return null;
    }
    return new LocalFileBackend(DocumentFile.fromFile(accountFolder), getContext());
  }

  private boolean hasLocalChanges(ContentProviderClient provider, Uri changesUri) throws RemoteException {
    boolean result = false;
    Cursor c = provider.query(changesUri,
        new String[]{"count(*)"}, null, null, null);

    if (c != null) {
      result = c.getCount() > 0;
      c.close();
    }
    return result;
  }
}
