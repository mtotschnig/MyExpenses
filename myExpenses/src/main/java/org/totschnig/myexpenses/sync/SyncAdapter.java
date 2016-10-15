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
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.provider.DocumentFile;
import android.support.v4.util.Pair;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.export.CategoryInfo;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.AcraHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.ACCOUNT_SERVICE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_FROM_ADAPTER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_REMOTE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final String TAG = "SyncAdapter";
  private Map<String, Long> categoryToId;
  private Map<String, Long> payeeToId;
  private Map<String, Long> methodToId;
  private Map<String, Long> accountUuidToId;

  private static final ThreadLocal<org.totschnig.myexpenses.model.Account>
      dbAccount = new ThreadLocal<org.totschnig.myexpenses.model.Account>();

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
    categoryToId = new HashMap<>();
    payeeToId = new HashMap<>();
    methodToId = new HashMap<>();
    accountUuidToId = new HashMap<>();
    Log.i(TAG, "onPerformSync");
    AccountManager accountManager = (AccountManager) getContext().getSystemService(ACCOUNT_SERVICE);
    String lastLocalSequence = getUserDataWithDefault(accountManager, account,
        KEY_SYNC_SEQUENCE_LOCAL, "0");
    long currentSequenceLocal = Long.parseLong(lastLocalSequence);
    String lastRemoteSequence = getUserDataWithDefault(accountManager, account,
        KEY_SYNC_SEQUENCE_REMOTE, "0");
    long currentSequenceRemote = Long.parseLong(lastRemoteSequence);
    String accountId = account.name.substring(1);
    dbAccount.set(org.totschnig.myexpenses.model.Account.getInstanceFromDb(Long.valueOf(accountId)));
    SyncBackend backend = getBackendForAccount(accountId);
    if (backend == null) {
      //TODO report
      return;
    }
    ChangeSet changeSetSince = backend.getChangeSetSince(Long.parseLong(lastRemoteSequence));

    List<TransactionChange> remoteChanges;
    List<TransactionChange> localChanges = new ArrayList<>();
    if (changeSetSince != null) {
      currentSequenceRemote = changeSetSince.sequenceNumber;
      remoteChanges = changeSetSince.changes;
    } else {
      remoteChanges = new ArrayList<>();
    }
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
    } catch (RemoteException e) {
      e.printStackTrace();
    }

    if (localChanges.size() > 0 && remoteChanges.size() > 0) {
      Pair<List<TransactionChange>, List<TransactionChange>> mergeResult =
          mergeChangeSets(localChanges, remoteChanges);
      localChanges = mergeResult.first; remoteChanges = mergeResult.second;
    }

    if (remoteChanges.size() > 0) {
      try {
        writeRemoteChangesToDb(provider, remoteChanges, accountId);
        accountManager.setUserData(account, KEY_SYNC_SEQUENCE_REMOTE, String.valueOf(currentSequenceRemote));
      } catch (RemoteException | OperationApplicationException | SQLiteException e) {
        AcraHelper.report(e);
        return;
      }
    }

    if (localChanges.size() > 0) {
      backend.lock();
      currentSequenceRemote = backend.writeChangeSet(localChanges);
      backend.unlock();
      if (currentSequenceRemote != ChangeSet.FAILED) {
        accountManager.setUserData(account, KEY_SYNC_SEQUENCE_LOCAL, String.valueOf(currentSequenceLocal));
        accountManager.setUserData(account, KEY_SYNC_SEQUENCE_REMOTE, String.valueOf(currentSequenceRemote));
      }
    }

  }

  private void writeRemoteChangesToDb(ContentProviderClient provider, List<TransactionChange> remoteChanges, String accountId)
      throws RemoteException, OperationApplicationException {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    Uri accountUri = TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(accountId).build();
    ops.add(ContentProviderOperation.newUpdate(accountUri).withValue(KEY_SYNC_FROM_ADAPTER, true).build());
    Stream.of(remoteChanges).filter(change -> !(change.isCreate() && uuidExists(change.uuid())))
        .forEach(change -> collectOperations(change, ops));
    ops.add(ContentProviderOperation.newUpdate(accountUri).withValue(KEY_SYNC_FROM_ADAPTER, false).build());
    provider.applyBatch(ops);
  }

  private boolean uuidExists(String uuid) {
    return Transaction.countPerUuid(uuid) > 0;
  }

  @VisibleForTesting
  public void collectOperations(@NonNull TransactionChange change, ArrayList<ContentProviderOperation> ops) {
    switch(change.type()) {
      case created:
        ops.addAll(toTransaction(change).buildSaveOperations());
        break;
      case updated:
        ops.add(ContentProviderOperation.newUpdate(TransactionProvider.TRANSACTIONS_URI)
            .withSelection(KEY_UUID + " = ?",new String[]{change.uuid()})
            .withValues(toContentValues(change))
            .build());
        break;
      case deleted:
       ops.add(ContentProviderOperation.newDelete(TransactionProvider.TRANSACTIONS_URI)
           .withSelection(KEY_UUID + " = ?",new String[]{change.uuid()})
           .build());
        break;
    }
  }

  private Transaction toTransaction(TransactionChange change) {
    if (!change.isCreate()) throw new AssertionError();
    Long amount = change.amount() != null ? change.amount() : 0L;
    Transaction t;
    if (change.transferAccount() != null) {
      long transferAccount = extractTransferAccount(change.transferAccount(),change.label());
      t = new Transfer(getAccount().getId(),amount);
      t.transfer_account = transferAccount;
    } else {
      t = new Transaction(getAccount().getId(), amount);
      if (change.label() != null) {
        t.setCatId(extractCatId(change.label()));
      }
    }
    if (change.comment() != null) {
      t.comment = change.comment();
    }
    if (change.date() != null) {
      Long date = change.date();
      assert date != null;
      t.setDate(new Date(date * 10000));
    }

    if (change.payeeName() != null) {
      long id = extractPayeeId(change.payeeName());
      if (id != -1) {
        t.payeeId = id;
      }
    }
    if (change.methodLabel() != null) {
      long id = extractMethodId(change.methodLabel());
      if (id != -1) {
        t.methodId = id;
      }
    }
    //values.put("transfer_account", transferAccount());
    if (change.crStatus() != null) {
      t.crStatus = Transaction.CrStatus.valueOf(change.crStatus());
    }
    t.referenceNumber = change.referenceNumber();
    return t;
  }

  private ContentValues toContentValues(TransactionChange change) {
    if (!change.isUpdate()) throw new AssertionError();
    ContentValues values = new ContentValues();
    //values.put("parent_uuid", parentUuid());
    if (change.comment() != null) {
      values.put(KEY_COMMENT, change.comment());
    }
    values.put(KEY_DATE, change.date());
    values.put(KEY_AMOUNT, change.amount());
    if (change.label() != null) {
      values.put(KEY_CATID, extractCatId(change.label()));
    }
    if (change.payeeName() != null) {
      long id = extractPayeeId(change.payeeName());
      if (id != -1) {
        values.put(KEY_PAYEEID, id);
      }
    }
    if (change.methodLabel() != null) {
      long id = extractMethodId(change.methodLabel());
        if (id != -1) {
          values.put(KEY_METHODID, id);
      }
    }
    //values.put("transfer_account", transferAccount());
    values.put(KEY_CR_STATUS, change.crStatus());
    values.put(KEY_REFERENCE_NUMBER, change.referenceNumber());
    //values.put("picture_id", pictureUri());
    return values;
  }

  private long extractTransferAccount(String uuid, String label) {
    Long id = accountUuidToId.get(uuid);
    if (id == null) {
      id = org.totschnig.myexpenses.model.Account.findByUuid(uuid);
      if (id == -1) {
        org.totschnig.myexpenses.model.Account transferAccount =
            new org.totschnig.myexpenses.model.Account(label, getAccount().currency, 0L, "",
            AccountType.CASH, org.totschnig.myexpenses.model.Account.DEFAULT_COLOR);
        transferAccount.uuid = uuid;
        transferAccount.save();
        id = transferAccount.getId();
      }
      if (id != -1) { //should always be the case
        accountUuidToId.put(uuid, id);
      }
    }
    return id;
  }

  private long extractCatId(String label) {
    new CategoryInfo(label).insert(categoryToId);
    return categoryToId.get(label);
  }

  private long extractPayeeId(String payeeName) {
    Long id = payeeToId.get(payeeName);
    if (id == null) {
      id = Payee.find(payeeName);
      if (id == -1) {
        id = Payee.maybeWrite(payeeName);
      }
      if (id != -1) { //should always be the case
        payeeToId.put(payeeName, id);
      }
    }
    return id;
  }

  private long extractMethodId(String methodLabel) {
    Long id = methodToId.get(methodLabel);
    if (id == null) {
      id = PaymentMethod.find(methodLabel);
      if (id == -1) {
        id = PaymentMethod.maybeWrite(methodLabel, getAccount().type);
      }
      if (id != -1) { //should always be the case
        methodToId.put(methodLabel, id);
      }
    }
    return id;
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
        .filter(uuid -> updatesPerUuid.get(uuid).size() > 1).collect(Collectors.toList());
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

  @VisibleForTesting
  public org.totschnig.myexpenses.model.Account getAccount() {
    return dbAccount.get();
  }
}
