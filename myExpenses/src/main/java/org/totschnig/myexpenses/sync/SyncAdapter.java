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
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.VisibleForTesting;
import android.support.v4.provider.DocumentFile;
import android.support.v4.util.Pair;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.ACCOUNT_SERVICE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_REMOTE;

class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final String TAG = "SyncAdapter";

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
    super(context, autoInitialize, allowParallelSyncs);
  }

  private String getUserDataWithDefault(AccountManager accountManager, Account account,
                                        String key, String defaultValue) {
    String value = accountManager.getUserData(account, key);
    return value == null ? defaultValue : value;
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider, SyncResult syncResult) {
    Log.i(TAG, "onPerformSync");
    AccountManager accountManager = (AccountManager) getContext().getSystemService(ACCOUNT_SERVICE);
    String lastLocalSequence = getUserDataWithDefault(accountManager, account,
        KEY_SYNC_SEQUENCE_LOCAL, "0");
    long currentSequenceLocal = Long.parseLong(lastLocalSequence);
    String lastRemoteSequence = getUserDataWithDefault(accountManager, account,
        KEY_SYNC_SEQUENCE_REMOTE, "0");
    long currentSequenceRemote = Long.parseLong(lastRemoteSequence);
    String accountId = account.name.substring(1);
    SyncBackend backend = getBackendForAccount(accountId);
    if (backend == null) {
      //TODO report
      return;
    }
    ChangeSet changeSetSince = backend.getChangeSetSince(Long.parseLong(lastRemoteSequence));

    if (changeSetSince != null) {
      currentSequenceRemote = changeSetSince.sequenceNumber;
      List<TransactionChange> remoteChanges = changeSetSince.changes;
    }

    List<TransactionChange> localChanges = new ArrayList<>();
    try {
      Uri changesUri = buildChangesUri(lastLocalSequence, accountId);
      if (hasLocalChanges(provider, changesUri)) {
        currentSequenceLocal++;
        ContentValues currentSyncIncrease = new ContentValues(1);
        currentSyncIncrease.put(KEY_SYNC_SEQUENCE_LOCAL, currentSequenceLocal);
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
      }

      if (localChanges.size() > 0) {
        backend.lock();
        currentSequenceRemote = backend.writeChangeSet(localChanges);
        backend.unlock();
        if (currentSequenceRemote != ChangeSet.FAILED) {
          accountManager.setUserData(account, KEY_SYNC_SEQUENCE_LOCAL, String.valueOf(currentSequenceLocal));
        }
      }

      //write remote changes to local

      accountManager.setUserData(account, KEY_SYNC_SEQUENCE_REMOTE, String.valueOf(currentSequenceRemote));
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

  @VisibleForTesting
  public Pair<List<TransactionChange>, List<TransactionChange>> mergeChangeSets(
      List<TransactionChange> first, List<TransactionChange> second) {

    //filter out changes made obsolete by later delete
    List<String> deletedUuids = Stream.concat(Stream.of(first), Stream.of(second))
        .filter(TransactionChange::isDelete)
        .map(TransactionChange::uuid)
        .collect(Collectors.toList());
    List<TransactionChange> firstResult = filterDeleted(first, deletedUuids);
    List<TransactionChange> secondResult = filterDeleted(second, deletedUuids);

    //merge update changes
    HashMap<String, List<TransactionChange>> updatesPerUuid = new HashMap<>();
    HashMap<String, TransactionChange> mergesPerUuid = new HashMap<>();
    Stream.concat(Stream.of(firstResult), Stream.of(secondResult))
        .forEach(change -> ensureList(updatesPerUuid, change.uuid()).add(change));
    List<String> uuidsRequiringMerge = Stream.of(updatesPerUuid.keySet())
        .filter(uuid -> updatesPerUuid.get(uuid).size() > 0).collect(Collectors.toList());
    Stream.of(uuidsRequiringMerge)
        .forEach(uuid -> mergesPerUuid.put(uuid, mergeUpdates(updatesPerUuid.get(uuid))));
    firstResult = replaceByMerged(firstResult, mergesPerUuid);
    secondResult = replaceByMerged(secondResult, mergesPerUuid);

    return Pair.create(firstResult, secondResult);
  }

  private List<TransactionChange> filterDeleted(List<TransactionChange> input, List<String> deletedUuids) {
    return Stream.of(input).filter(change ->
        change.isDelete() || !deletedUuids.contains(change.uuid()))
        .collect(Collectors.toList());
  }

  private List<TransactionChange> replaceByMerged(List<TransactionChange> input, HashMap<String, TransactionChange> mergedMap) {
    return Stream.of(input).map(change -> change.type().equals(TransactionChange.Type.updated.name())
        && mergedMap.containsKey(change.uuid()) ?
        mergedMap.get(change.uuid()) : change)
        .distinct().collect(Collectors.toList());
  }

  @VisibleForTesting
  public TransactionChange mergeUpdates(List<TransactionChange> changeList) {
    if (changeList.size() < 2) {
      throw new IllegalStateException("nothing to merge");
    }
    return Stream.of(changeList).sortBy(TransactionChange::timeStamp).reduce(TransactionChange::mergeUpdate).get();
  }

  protected Uri buildChangesUri(String current_sync, String accountId) {
    return TransactionProvider.CHANGES_URI.buildUpon()
        .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, accountId)
        .appendQueryParameter(KEY_SYNC_SEQUENCE_LOCAL, current_sync)
        .build();
  }

  private List<TransactionChange> ensureList(HashMap<String, List<TransactionChange>> map, String uuid) {
    List<TransactionChange> changesForUuid = map.get(uuid);
    if (changesForUuid == null) {
      changesForUuid = new ArrayList<>();
      map.put(uuid, changesForUuid);
    }
    return changesForUuid;
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
      if (c.moveToFirst()) {
        result = c.getLong(0) > 0;
      }
      c.close();
    }
    return result;
  }
}
