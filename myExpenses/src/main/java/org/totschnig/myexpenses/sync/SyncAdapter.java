package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;
import android.util.SparseArray;

import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;
import com.annimon.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.export.CategoryInfo;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.SyncNotificationDismissHandler;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final int BATCH_SIZE = 100;
  public static final String KEY_RESET_REMOTE_ACCOUNT = "reset_remote_account";
  public static final String KEY_UPLOAD_AUTO_BACKUP_URI = "upload_auto_backup_uri";
  public static final String KEY_UPLOAD_AUTO_BACKUP_NAME = "upload_auto_backup_name";
  public static final String KEY_NOTIFICATION_CANCELLED = "notification_cancelled";
  private static final ThreadLocal<org.totschnig.myexpenses.model.Account>
      dbAccount = new ThreadLocal<>();
  public static final int LOCK_TIMEOUT_MINUTES = BuildConfig.DEBUG ? 1 : 30;
  private static final long IO_DEFAULT_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(5);
  private static final long IO_LOCK_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(LOCK_TIMEOUT_MINUTES);
  private Map<String, Long> categoryToId;
  private Map<String, Long> payeeToId;
  private Map<String, Long> methodToId;
  private Map<String, Long> accountUuidToId;
  private SparseArray<StringBuilder> notificationContent = new SparseArray<>();
  public static final String TAG = "SyncAdapter";
  private boolean shouldNotify = true;

  public SyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
    super(context, autoInitialize, allowParallelSyncs);
  }

  public static String KEY_LAST_SYNCED_REMOTE(long accountId) {
    return "last_synced_remote_" + accountId;
  }

  public static String KEY_LAST_SYNCED_LOCAL(long accountId) {
    return "last_synced_local_" + accountId;
  }

  private String getUserDataWithDefault(AccountManager accountManager, Account account,
                                        String key, String defaultValue) {
    String value = accountManager.getUserData(account, key);
    return value == null ? defaultValue : value;
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider, SyncResult syncResult) {
    Timber.i("onPerformSync %s", extras);
    if (extras.getBoolean(KEY_NOTIFICATION_CANCELLED)) {
      notificationContent.remove(account.hashCode());
      if (ContentResolver.isSyncPending(account, authority)) {
        ContentResolver.cancelSync(account, authority);
      }
      return;
    }
    categoryToId = new HashMap<>();
    payeeToId = new HashMap<>();
    methodToId = new HashMap<>();
    accountUuidToId = new HashMap<>();
    String uuidFromExtras = extras.getString(KEY_UUID);
    int notificationId = account.hashCode();
    if (notificationContent.get(notificationId) == null) {
      notificationContent.put(notificationId, new StringBuilder());
    }

    AccountManager accountManager = AccountManager.get(getContext());

    Exceptional<SyncBackendProvider> backendProviderExceptional =
        SyncBackendProviderFactory.get(getContext(), account);
    SyncBackendProvider backend;
    try {
      backend = backendProviderExceptional.getOrThrow();
    } catch (Throwable throwable) {
      syncResult.databaseError = true;
      CrashHandler.report(throwable);
      GenericAccountService.deactivateSync(account);
      accountManager.setUserData(account, GenericAccountService.KEY_BROKEN, "1");
      notifyUser("Synchronization backend deactivated",
          String.format(Locale.ROOT,
              "The backend could not be instantiated. Reason: %s. Please try to delete and recreate it.",
              throwable.getMessage()),
          null,
          getManageSyncBackendsIntent());

      return;
    }
    String authToken;
    try {
      authToken = accountManager.blockingGetAuthToken(account, GenericAccountService.Authenticator.AUTH_TOKEN_TYPE,
          true);
    } catch (OperationCanceledException | IOException | AuthenticatorException e) {
      log().e("Error getting auth token.", e);
      syncResult.stats.numAuthExceptions++;
      return;
    }
    if (!backend.setUp(authToken).success) {
      syncResult.stats.numIoExceptions++;
      syncResult.delayUntil = IO_DEFAULT_DELAY_SECONDS;
      appendToNotification(Utils.concatResStrings(getContext(), " ",
          R.string.sync_io_error_cannot_connect, R.string.sync_error_will_try_again_later), account, true);
      return;
    }

    String autoBackupFileUri = extras.getString(KEY_UPLOAD_AUTO_BACKUP_URI);
    if (autoBackupFileUri != null) {
      String fileName = extras.getString(KEY_UPLOAD_AUTO_BACKUP_NAME);
      try {
        backend.storeBackup(Uri.parse(autoBackupFileUri), fileName);
      } catch (IOException e) {
        log().w(e);
        if (handleAuthException(backend, e, account)) {
          return;
        }
        notifyUser(getContext().getString(R.string.pref_auto_backup_title),
            getContext().getString(R.string.auto_backup_cloud_failure, fileName, account.name)
                + " " + e.getMessage(), null, null);
      }
      return;
    }

    Cursor cursor;

    try {
      cursor = provider.query(TransactionProvider.SETTINGS_URI, new String[]{KEY_VALUE},
          KEY_KEY + " = ?", new String[]{PrefKey.SYNC_NOTIFICATION.getKey()}, null);
      if (cursor != null) {
        if (cursor.moveToFirst()) {
          shouldNotify = cursor.getString(0).equals(Boolean.TRUE.toString());
        }
        cursor.close();
      }
    } catch (RemoteException ignored) {
    }


    String[] selectionArgs;
    String selection = KEY_SYNC_ACCOUNT_NAME + " = ?";
    if (uuidFromExtras != null) {
      selection += " AND " + KEY_UUID + " = ?";
      selectionArgs = new String[]{account.name, uuidFromExtras};
    } else {
      selectionArgs = new String[]{account.name};
    }
    String[] projection = {KEY_ROWID};

    try {
      cursor = provider.query(TransactionProvider.ACCOUNTS_URI, projection,
          selection + " AND " + KEY_SYNC_SEQUENCE_LOCAL + " = 0", selectionArgs, null);
    } catch (RemoteException e) {
      syncResult.databaseError = true;
      notifyDatabaseError(e, account);
      return;
    }
    if (cursor == null) {
      syncResult.databaseError = true;
      Exception exception = new Exception("Cursor is null");
      notifyDatabaseError(exception, account);
      return;
    }
    if (cursor.moveToFirst()) {
      do {
        long accountId = cursor.getLong(0);
        try {
          provider.update(buildInitializationUri(accountId), new ContentValues(0), null, null);
          //make sure user data did not stick around after a user might have cleared data
          accountManager.setUserData(account, KEY_LAST_SYNCED_LOCAL(accountId), null);
          accountManager.setUserData(account, KEY_LAST_SYNCED_REMOTE(accountId), null);
        } catch (RemoteException e) {
          syncResult.databaseError = true;
          notifyDatabaseError(e, account);
          return;
        }
      } while (cursor.moveToNext());
    }
    cursor.close();

    try {
      cursor = provider.query(TransactionProvider.ACCOUNTS_URI, projection, selection, selectionArgs,
          null);
    } catch (RemoteException e) {
      syncResult.databaseError = true;
      notifyDatabaseError(e, account);
      return;
    }
    if (cursor != null) {
      if (cursor.moveToFirst()) {
        do {
          long accountId = cursor.getLong(0);

          String lastLocalSyncKey = KEY_LAST_SYNCED_LOCAL(accountId);
          String lastRemoteSyncKey = KEY_LAST_SYNCED_REMOTE(accountId);

          long lastSyncedLocal = Long.parseLong(getUserDataWithDefault(accountManager, account,
              lastLocalSyncKey, "0"));
          long lastSyncedRemote = Long.parseLong(getUserDataWithDefault(accountManager, account,
              lastRemoteSyncKey, "0"));
          dbAccount.set(org.totschnig.myexpenses.model.Account.getInstanceFromDb(accountId));
          appendToNotification(getContext().getString(R.string.synchronization_start, dbAccount.get().getLabel()), account, true);
          if (uuidFromExtras != null && extras.getBoolean(KEY_RESET_REMOTE_ACCOUNT)) {
            try {
              backend.resetAccountData(uuidFromExtras);
            } catch (IOException e) {
              log().w(e);
              if (handleAuthException(backend, e, account)) {
                return;
              }
              syncResult.stats.numIoExceptions++;
              syncResult.delayUntil = IO_DEFAULT_DELAY_SECONDS;
              notifyIoException(R.string.sync_io_exception_reset_account_data, account);
            }
            break;
          }

          try {
            backend.withAccount(dbAccount.get());
          } catch (IOException e) {
            log().w(e);
            if (handleAuthException(backend, e, account)) {
              return;
            }
            syncResult.stats.numIoExceptions++;
            syncResult.delayUntil = IO_DEFAULT_DELAY_SECONDS;
            notifyIoException(R.string.sync_io_exception_setup_remote_account, account);
            continue;
          }

          try {
            backend.lock();
          } catch (IOException e) {
            log().w(e);
            if (handleAuthException(backend, e, account)) {
              return;
            }
            notifyIoException(R.string.sync_io_exception_locking, account);
            syncResult.stats.numIoExceptions++;
            syncResult.delayUntil = IO_LOCK_DELAY_SECONDS;
            continue;
          }

          boolean completedWithoutError = false;
          int successRemote2Local = 0, successLocal2Remote = 0;
          try {
            ChangeSet changeSetSince = backend.getChangeSetSince(lastSyncedRemote, getContext());

            if (changeSetSince.isFailed()) {
              syncResult.stats.numIoExceptions++;
              syncResult.delayUntil = IO_DEFAULT_DELAY_SECONDS;
              notifyIoException(R.string.sync_io_exception_reading_change_set, account);
              continue;
            }

            List<TransactionChange> remoteChanges;
            lastSyncedRemote = changeSetSince.sequenceNumber;
            remoteChanges = changeSetSince.changes;

            List<TransactionChange> localChanges = new ArrayList<>();
            long sequenceToTest = lastSyncedLocal + 1;
            while (true) {
              List<TransactionChange> nextChanges = getLocalChanges(provider, accountId, sequenceToTest);
              if (nextChanges.size() > 0) {
                localChanges.addAll(Stream.of(nextChanges).filter(change -> !change.isEmpty()).toList());
                lastSyncedLocal = sequenceToTest;
                sequenceToTest++;
              } else {
                break;
              }
            }

            if (localChanges.size() > 0 || remoteChanges.size() > 0) {

              if (localChanges.size() > 0) {
                localChanges = collectSplits(localChanges);
              }

              Pair<List<TransactionChange>, List<TransactionChange>> mergeResult =
                  mergeChangeSets(localChanges, remoteChanges);
              localChanges = mergeResult.first;
              remoteChanges = mergeResult.second;

              if (remoteChanges.size() > 0) {
                writeRemoteChangesToDb(provider, remoteChanges, accountId);
                accountManager.setUserData(account, lastRemoteSyncKey, String.valueOf(lastSyncedRemote));
                successRemote2Local = remoteChanges.size();
              }

              if (localChanges.size() > 0) {
                lastSyncedRemote = backend.writeChangeSet(lastSyncedRemote, localChanges, getContext());
                if (lastSyncedRemote != ChangeSet.FAILED) {
                  if (!BuildConfig.DEBUG) {
                    // on debug build for auditing purposes, we keep changes in the table
                    provider.delete(TransactionProvider.CHANGES_URI,
                        KEY_ACCOUNTID + " = ? AND " + KEY_SYNC_SEQUENCE_LOCAL + " <= ?",
                        new String[]{String.valueOf(accountId), String.valueOf(lastSyncedLocal)});
                  }
                  accountManager.setUserData(account, lastLocalSyncKey, String.valueOf(lastSyncedLocal));
                  accountManager.setUserData(account, lastRemoteSyncKey, String.valueOf(lastSyncedRemote));
                  successLocal2Remote = localChanges.size();
                }
              }
            }
            completedWithoutError = true;
          } catch (IOException e) {
            log().w(e);
            if (handleAuthException(backend, e, account)) {
              return;
            }
            syncResult.stats.numIoExceptions++;
            syncResult.delayUntil = IO_DEFAULT_DELAY_SECONDS;
            notifyIoException(R.string.sync_io_exception_syncing, account);
          } catch (RemoteException | OperationApplicationException | SQLiteException e) {
            syncResult.databaseError = true;
            notifyDatabaseError(e, account);
          } catch (Exception e) {
            appendToNotification("ERROR: " + e.getMessage(), account, true);
          } finally {
            if (successLocal2Remote > 0 || successRemote2Local > 0) {
              appendToNotification(getContext().getString(R.string.synchronization_end_success, successRemote2Local, successLocal2Remote), account, false);
            } else if (completedWithoutError) {
              appendToNotification(getContext().getString(R.string.synchronization_end_success_none), account, false);
            }
            try {
              backend.unlock();
            } catch (IOException e) {
              log().w(e);
              if (!handleAuthException(backend, e, account)) {
                notifyIoException(R.string.sync_io_exception_unlocking, account);
                syncResult.stats.numIoExceptions++;
                syncResult.delayUntil = IO_LOCK_DELAY_SECONDS;
              }
            }
          }
        } while (cursor.moveToNext());
      }
      cursor.close();
    }
    backend.tearDown();
  }

  private boolean handleAuthException(SyncBackendProvider backend, IOException e, Account account) {
    if (backend.isAuthException(e)) {
      backend.tearDown();
      Intent manageSyncBackendsIntent = getManageSyncBackendsIntent();
      manageSyncBackendsIntent.setAction(ManageSyncBackends.ACTION_REFRESH_LOGIN);
      manageSyncBackendsIntent.putExtra(KEY_SYNC_ACCOUNT_NAME, account.name);
      notifyUser(getContext().getString(R.string.sync_auth_exception_login_again), null, null, manageSyncBackendsIntent);
      return true;
    }
    return false;
  }

  @NonNull
  private Intent getManageSyncBackendsIntent() {
    return new Intent(getContext(), ManageSyncBackends.class);
  }

  private void appendToNotification(String content, Account account, boolean newLine) {
    log().i(content);
    if (shouldNotify) {
      StringBuilder contentBuilder = notificationContent.get(account.hashCode());
      if (contentBuilder.length() > 0) {
        contentBuilder.append(newLine ? "\n" : " ");
      }
      contentBuilder.append(content);
      content = contentBuilder.toString();
    }
    notifyUser(getNotificationTitle(), content, account, null);
  }

  public static Timber.Tree log() {
    return Timber.tag(TAG);
  }

  private void notifyUser(String title, String content, @Nullable Account account, @Nullable Intent intent) {
    if (shouldNotify) {
      NotificationBuilderWrapper builder = NotificationBuilderWrapper.bigTextStyleBuilder(
          getContext(), NotificationBuilderWrapper.CHANNEL_ID_SYNC, title, content);
      if (intent != null) {
        builder.setContentIntent(PendingIntent.getActivity(
            getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
      }
      if (account != null) {
        Intent dismissIntent = new Intent(getContext(), SyncNotificationDismissHandler.class);
        dismissIntent.putExtra(KEY_SYNC_ACCOUNT_NAME, account.name);
        builder.setDeleteIntent(PendingIntent.getService(getContext(), 0,
            dismissIntent, PendingIntent.FLAG_ONE_SHOT));
      }
      Notification notification = builder.build();
      notification.flags = Notification.FLAG_AUTO_CANCEL;
      ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(
          "SYNC", account != null ? account.hashCode() : 0, notification);
    }
  }

  private void notifyIoException(int resId, Account account) {
    appendToNotification(getContext().getString(resId), account, true);
  }

  private void notifyDatabaseError(Exception e, Account account) {
    CrashHandler.report(e);
    appendToNotification(getContext().getString(R.string.sync_database_error) + " " + e.getMessage(),
        account, true);
  }

  private String getNotificationTitle() {
    return Utils.concatResStrings(getContext(), " ", R.string.app_name, R.string.synchronization);
  }

  private List<TransactionChange> getLocalChanges(ContentProviderClient provider, long accountId,
                                                  long sequenceNumber) throws RemoteException {
    List<TransactionChange> result = new ArrayList<>();
    Uri changesUri = buildChangesUri(sequenceNumber, accountId);
    boolean hasLocalChanges = hasLocalChanges(provider, changesUri);
    if (hasLocalChanges) {
      ContentValues currentSyncIncrease = new ContentValues(1);
      long nextSequence = sequenceNumber + 1;
      currentSyncIncrease.put(KEY_SYNC_SEQUENCE_LOCAL, nextSequence);
      //in case of failed syncs due to non-available backends, sequence number might already be higher than nextSequence
      //we must take care to not decrease it here
      provider.update(TransactionProvider.ACCOUNTS_URI, currentSyncIncrease, KEY_ROWID + " = ? AND " + KEY_SYNC_SEQUENCE_LOCAL + " < ?",
          new String[]{String.valueOf(accountId), String.valueOf(nextSequence)});
    }
    if (hasLocalChanges) {
      Cursor c = provider.query(changesUri, null, null, null, null);
      if (c != null) {
        if (c.moveToFirst()) {
          do {
            TransactionChange transactionChange = TransactionChange.create(c);
            result.add(transactionChange);
          } while (c.moveToNext());
        }
        c.close();
      }
    }
    return result;
  }

  /**
   * @param changeList
   * @return the same list with split parts moved as parts to their parents. If there are multiple parents
   * for the same uuid, the splits will appear under each of them
   */
  private List<TransactionChange> collectSplits(List<TransactionChange> changeList) {
    HashMap<String, List<TransactionChange>> splitsPerUuid = new HashMap<>();
    for (Iterator<TransactionChange> i = changeList.iterator(); i.hasNext(); ) {
      TransactionChange change = i.next();
      if ((change.parentUuid() != null)) {
        ensureList(splitsPerUuid, change.parentUuid()).add(change);
        i.remove();
      }
    }
    //When a split transaction is changed, we do not necessarily have an entry for the parent, so we
    //create one here
    Stream.of(splitsPerUuid.keySet()).forEach(uuid -> {
      if (!Stream.of(changeList).anyMatch(change -> change.uuid().equals(uuid))) {
        changeList.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setTimeStamp(splitsPerUuid.get(uuid).get(0).timeStamp()).setUuid(uuid).build());
        splitsPerUuid.put(uuid, filterDeleted(
            splitsPerUuid.get(uuid), findDeletedUuids(Stream.of(splitsPerUuid.get(uuid)))));
      }
    });

    return Stream.of(changeList).map(change -> splitsPerUuid.containsKey(change.uuid()) ?
        change.toBuilder().setSplitPartsAndValidate(splitsPerUuid.get(change.uuid())).build() : change)
        .collect(Collectors.toList());
  }

  private void writeRemoteChangesToDb(ContentProviderClient provider, List<TransactionChange> remoteChanges, long accountId)
      throws RemoteException, OperationApplicationException {
    if (remoteChanges.size() == 0) {
      return;
    }
    if (remoteChanges.size() > BATCH_SIZE) {
      for (List<TransactionChange> part : ListUtils.partition(remoteChanges, BATCH_SIZE)) {
        writeRemoteChangesToDbPart(provider, part, accountId);
      }
    } else {
      writeRemoteChangesToDbPart(provider, remoteChanges, accountId);
    }
  }

  private void writeRemoteChangesToDbPart(ContentProviderClient provider, List<TransactionChange> remoteChanges, long accountId) throws RemoteException, OperationApplicationException {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ops.add(ContentProviderOperation.newInsert(
        TransactionProvider.DUAL_URI.buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_SYNC_BEGIN, "1").build())
        .build());
    Stream.of(remoteChanges)
        .forEach(change -> collectOperations(change, accountId, ops, -1));
    ops.add(ContentProviderOperation.newDelete(
        TransactionProvider.DUAL_URI.buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_SYNC_END, "1").build())
        .build());
    ContentProviderResult[] contentProviderResults = provider.applyBatch(ops);
    int opsSize = ops.size();
    int resultsSize = contentProviderResults.length;
    if (opsSize != resultsSize) {
      CrashHandler.report(String.format(Locale.ROOT, "applied %d operations, received %d results",
          opsSize, resultsSize));
    }
  }

  @VisibleForTesting
  public void collectOperations(@NonNull TransactionChange change, long accountId, ArrayList<ContentProviderOperation> ops, int parentOffset) {
    Uri uri = Transaction.CALLER_IS_SYNC_ADAPTER_URI;
    boolean skipped = false;
    switch (change.type()) {
      case created:
        long transactionId = Transaction.findByAccountAndUuid(accountId, change.uuid());
        if (transactionId > -1) {
          if (parentOffset > -1) {
            //if we find a split part that already exists, we need to assume that it has already been synced
            //by a previous sync of its transfer account, so all we do here is reparent it as child
            //of the split transaction we currently ingest
            ops.add(ContentProviderOperation.newUpdate(uri)
                .withValues(toContentValues(change))
                .withSelection(KEY_ROWID + " = ?", new String[]{String.valueOf(transactionId)})
                .withValueBackReference(KEY_PARENTID, parentOffset)
                .build());
          } else {
            skipped = true;
            Timber.i("Uuid found in changes already exists locally, likely a transfer implicitly created from its peer");
          }
        } else {
          ops.addAll(getContentProviderOperationsForCreate(change, ops.size(), parentOffset));
        }
        break;
      case updated:
        ContentValues values = toContentValues(change);
        if (values.size() > 0) {
          ops.add(ContentProviderOperation.newUpdate(uri)
              .withSelection(KEY_UUID + " = ? AND " + KEY_ACCOUNTID + " = ?", new String[]{change.uuid(), String.valueOf(accountId)})
              .withValues(values)
              .build());
        }
        break;
      case deleted:
        ops.add(ContentProviderOperation.newDelete(uri)
            .withSelection(KEY_UUID + " = ?", new String[]{change.uuid()})
            .build());
        break;
    }
    if (change.splitParts() != null && !skipped) {
      final int newParentOffset = ops.size() - 1;
      List<TransactionChange> splitPartsFiltered = filterDeleted(change.splitParts(),
          findDeletedUuids(Stream.of(change.splitParts())));
      Stream.of(splitPartsFiltered).forEach(splitChange -> collectOperations(splitChange, accountId, ops,
          change.isCreate() ? newParentOffset : -1)); //back reference is only used when we insert a new split, for updating an existing split we search for its _id via its uuid
    }
  }


  private ArrayList<ContentProviderOperation> getContentProviderOperationsForCreate(
      TransactionChange change, int offset, int parentOffset) {
    if (!change.isCreate()) throw new AssertionError();
    Long amount;
    if (change.amount() != null) {
      amount = change.amount();
    } else {
      amount = 0L;
    }
    Money money = new Money(getAccount().currency, amount);
    Transaction t;
    long transferAccount;
    if (change.splitParts() != null) {
      t = new SplitTransaction(getAccount().getId(), money);
    } else if (change.transferAccount() != null &&
        (transferAccount = extractTransferAccount(change.transferAccount(), change.label())) != -1) {
      t = new Transfer(getAccount().getId(), money, transferAccount);
    } else {
      t = new Transaction(getAccount().getId(), money);
      if (change.label() != null) {
        long catId = extractCatId(change.label());
        if (catId != -1) {
          t.setCatId(catId);
        }
      }
    }
    t.uuid = change.uuid();
    if (change.comment() != null) {
      t.setComment(change.comment());
    }
    if (change.date() != null) {
      Long date = change.date();
      assert date != null;
      t.setDate(new Date(date * 1000));
    }

    if (change.payeeName() != null) {
      long id = Payee.extractPayeeId(change.payeeName(), payeeToId);
      if (id != -1) {
        t.setPayeeId(id);
      }
    }
    if (change.methodLabel() != null) {
      long id = extractMethodId(change.methodLabel());
      if (id != -1) {
        t.setMethodId(id);
      }
    }
    if (change.crStatus() != null) {
      t.crStatus = Transaction.CrStatus.valueOf(change.crStatus());
    }
    t.setReferenceNumber(change.referenceNumber());
    if (parentOffset == -1 && change.parentUuid() != null) {
      long parentId = Transaction.findByAccountAndUuid(getAccount().getId(), change.parentUuid());
      if (parentId == -1) {
        return new ArrayList<>(); //if we fail to link a split part to a parent, we need to ignore it
      }
      t.setParentId(parentId);
    }
    if (change.pictureUri() != null) {
      t.setPictureUri(Uri.parse(change.pictureUri()));
    }
    return t.buildSaveOperations(offset, parentOffset, true);
  }

  private ContentValues toContentValues(TransactionChange change) {
    ContentValues values = new ContentValues();
    //values.put("parent_uuid", parentUuid());
    if (change.comment() != null) {
      values.put(KEY_COMMENT, change.comment());
    }
    if (change.date() != null) {
      values.put(KEY_DATE, change.date());
    }
    if (change.amount() != null) {
      values.put(KEY_AMOUNT, change.amount());
    }
    if (change.label() != null) {
      long catId = extractCatId(change.label());
      if (catId != -1) {
        values.put(KEY_CATID, catId);
      }
    }
    if (change.payeeName() != null) {
      long id = Payee.extractPayeeId(change.payeeName(), payeeToId);
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
    if (change.crStatus() != null) {
      values.put(KEY_CR_STATUS, change.crStatus());
    }
    if (change.referenceNumber() != null) {
      values.put(KEY_REFERENCE_NUMBER, change.referenceNumber());
    }
    if (change.pictureUri() != null) {
      values.put(KEY_PICTURE_URI, change.pictureUri());
    }
    return values;
  }

  private long extractTransferAccount(String uuid, String label) {
    Long id = accountUuidToId.get(uuid);
    if (id == null) {
      id = org.totschnig.myexpenses.model.Account.findByUuid(uuid);
      if (id == -1 && label != null) {
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
    new CategoryInfo(label).insert(categoryToId, false);
    return categoryToId.get(label) != null ? categoryToId.get(label) : -1;
  }

  private long extractMethodId(String methodLabel) {
    Long id = methodToId.get(methodLabel);
    if (id == null) {
      id = PaymentMethod.find(methodLabel);
      if (id == -1) {
        id = PaymentMethod.maybeWrite(methodLabel, getAccount().getType());
      }
      if (id != -1) { //should always be the case
        methodToId.put(methodLabel, id);
      }
    }
    return id;
  }

  @VisibleForTesting
  Pair<List<TransactionChange>, List<TransactionChange>> mergeChangeSets(
      List<TransactionChange> first, List<TransactionChange> second) {

    //filter out changes made obsolete by later delete
    List<String> deletedUuids = findDeletedUuids(Stream.concat(Stream.of(first), Stream.of(second)));

    List<TransactionChange> firstResult = filterDeleted(first, deletedUuids);
    List<TransactionChange> secondResult = filterDeleted(second, deletedUuids);

    //merge update changes
    HashMap<String, List<TransactionChange>> updatesPerUuid = new HashMap<>();
    HashMap<String, TransactionChange> mergesPerUuid = new HashMap<>();
    Stream.concat(Stream.of(firstResult), Stream.of(secondResult))
        .filter(TransactionChange::isCreateOrUpdate)
        .forEach(change -> ensureList(updatesPerUuid, change.uuid()).add(change));
    List<String> uuidsRequiringMerge = Stream.of(updatesPerUuid.keySet())
        .filter(uuid -> updatesPerUuid.get(uuid).size() > 1).collect(Collectors.toList());
    Stream.of(uuidsRequiringMerge)
        .forEach(uuid -> mergesPerUuid.put(uuid, mergeUpdates(updatesPerUuid.get(uuid))));
    firstResult = replaceByMerged(firstResult, mergesPerUuid);
    secondResult = replaceByMerged(secondResult, mergesPerUuid);

    return Pair.create(firstResult, secondResult);
  }

  private List<String> findDeletedUuids(Stream<TransactionChange> stream) {
    return stream.filter(TransactionChange::isDelete)
        .map(TransactionChange::uuid)
        .collect(Collectors.toList());
  }

  private List<TransactionChange> filterDeleted(List<TransactionChange> input, List<String> deletedUuids) {
    return Stream.of(input).filter(change ->
        change.isDelete() || !deletedUuids.contains(change.uuid()))
        .collect(Collectors.toList());
  }

  private List<TransactionChange> replaceByMerged(List<TransactionChange> input, HashMap<String, TransactionChange> mergedMap) {
    return Stream.of(input).map(change -> change.isCreateOrUpdate()
        && mergedMap.containsKey(change.uuid()) ? mergedMap.get(change.uuid()) : change)
        .distinct().collect(Collectors.toList());
  }

  @VisibleForTesting
  public TransactionChange mergeUpdates(List<TransactionChange> changeList) {
    if (changeList.size() < 2) {
      throw new IllegalStateException("nothing to merge");
    }
    return Stream.of(changeList).sortBy(TransactionChange::timeStamp).reduce(this::mergeUpdate).get();
  }

  private TransactionChange mergeUpdate(TransactionChange initial, TransactionChange change) {
    if (!(change.isCreateOrUpdate() && initial.isCreateOrUpdate())) {
      throw new IllegalStateException("Can only merge creates and updates");
    }
    if (!initial.uuid().equals(change.uuid())) {
      throw new IllegalStateException("Can only merge changes with same uuid");
    }
    TransactionChange.Builder builder = initial.toBuilder();
    if (change.parentUuid() != null) {
      builder.setParentUuid(change.parentUuid());
    }
    if (change.comment() != null) {
      builder.setComment(change.comment());
    }
    if (change.date() != null) {
      builder.setDate(change.date());
    }
    if (change.amount() != null) {
      builder.setAmount(change.amount());
    }
    if (change.label() != null) {
      builder.setLabel(change.label());
    }
    if (change.payeeName() != null) {
      builder.setPayeeName(change.payeeName());
    }
    if (change.transferAccount() != null) {
      builder.setTransferAccount(change.transferAccount());
    }
    if (change.methodLabel() != null) {
      builder.setMethodLabel(change.methodLabel());
    }
    if (change.crStatus() != null) {
      builder.setCrStatus(change.crStatus());
    }
    if (change.referenceNumber() != null) {
      builder.setReferenceNumber(change.referenceNumber());
    }
    if (change.pictureUri() != null) {
      builder.setPictureUri(change.pictureUri());
    }
    if (change.splitParts() != null) {
      builder.setSplitParts(change.splitParts());
    }
    return builder.setCurrentTimeStamp().build();
  }

  private Uri buildChangesUri(long current_sync, long accountId) {
    return TransactionProvider.CHANGES_URI.buildUpon()
        .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, String.valueOf(accountId))
        .appendQueryParameter(KEY_SYNC_SEQUENCE_LOCAL, String.valueOf(current_sync))
        .build();
  }

  private Uri buildInitializationUri(long accountId) {
    return TransactionProvider.CHANGES_URI.buildUpon()
        .appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, String.valueOf(accountId))
        .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_INIT, "1")
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
