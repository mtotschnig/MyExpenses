package org.totschnig.myexpenses.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.SparseArray;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.SyncNotificationDismissHandler;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.util.NotificationBuilderWrapper;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
  public static final int BATCH_SIZE = 100;
  public static final String KEY_RESET_REMOTE_ACCOUNT = "reset_remote_account";
  public static final String KEY_UPLOAD_AUTO_BACKUP_URI = "upload_auto_backup_uri";
  public static final String KEY_UPLOAD_AUTO_BACKUP_NAME = "upload_auto_backup_name";
  public static final String KEY_NOTIFICATION_CANCELLED = "notification_cancelled";
  private SyncDelegate syncDelegate;
  public static final int LOCK_TIMEOUT_MINUTES = BuildConfig.DEBUG ? 1 : 5;
  private static final long IO_DEFAULT_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(5);
  private static final long IO_LOCK_DELAY_SECONDS = TimeUnit.MINUTES.toSeconds(LOCK_TIMEOUT_MINUTES);
  private SparseArray<List<StringBuilder>> notificationContent = new SparseArray<>();
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

  private static long getIoDefaultDelaySeconds() {
    return (System.currentTimeMillis() / 1000) + IO_DEFAULT_DELAY_SECONDS;
  }

  private static long getIoLockDelaySeconds() {
    return (System.currentTimeMillis() / 1000) + IO_LOCK_DELAY_SECONDS;
  }

  private String getUserDataWithDefault(AccountManager accountManager, Account account,
                                        String key, String defaultValue) {
    String value = accountManager.getUserData(account, key);
    return value == null ? defaultValue : value;
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
                            ContentProviderClient provider, SyncResult syncResult) {
    log().i("onPerformSync %s", extras);
    if (extras.getBoolean(KEY_NOTIFICATION_CANCELLED)) {
      notificationContent.remove(account.hashCode());
      return;
    }
    syncDelegate = new SyncDelegate(getCurrencyConext());
    String uuidFromExtras = extras.getString(KEY_UUID);
    int notificationId = account.hashCode();
    if (notificationContent.get(notificationId) == null) {
      notificationContent.put(notificationId, new ArrayList<>());
    }

    shouldNotify = getBooleanSetting(provider, PrefKey.SYNC_NOTIFICATION, true);

    if (getBooleanSetting(provider, PrefKey.SYNC_WIFI_ONLY, false) && !isConnectedWifi(getContext())) {
      final String message = getContext().getString(R.string.wifi_not_connected);
      log().i(message);
      if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL)) {
        maybeNotifyUser(getNotificationTitle(), message, account);
      }
      return;
    }

    AccountManager accountManager = AccountManager.get(getContext());

    Exceptional<SyncBackendProvider> backendProviderExceptional =
        SyncBackendProviderFactory.get(getContext(), account, false);
    SyncBackendProvider backend;
    try {
      backend = backendProviderExceptional.getOrThrow();
    } catch (Throwable throwable) {
      if (throwable instanceof SyncBackendProvider.SyncParseException || throwable instanceof SyncBackendProvider.EncryptionException) {
        syncResult.databaseError = true;
        report(throwable);
        GenericAccountService.deactivateSync(account);
        accountManager.setUserData(account, GenericAccountService.KEY_BROKEN, "1");
        notifyUser("Synchronization backend deactivated",
            String.format(Locale.ROOT,
                "The backend could not be instantiated. Reason: %s. Please try to delete and recreate it.",
                throwable.getMessage()),
            null,
            getManageSyncBackendsIntent());
      } else if (throwable instanceof SyncBackendProvider.ResolvableSetupException) {
        notifyWithResolution((SyncBackendProvider.ResolvableSetupException) throwable);
      } else {
        if (throwable instanceof IOException) {
          log().i(throwable, "Error setting up account %s", account);
        } else {
          log().e(throwable, "Error setting up account %s", account);
        }
        syncResult.stats.numIoExceptions++;
        syncResult.delayUntil = getIoDefaultDelaySeconds();
        appendToNotification(TextUtils.concatResStrings(getContext(), " ",
            R.string.sync_io_error_cannot_connect, R.string.sync_error_will_try_again_later), account, true);
      }
      return;
    }

    handleAutoBackupSync(account, provider, backend);

    Cursor cursor;

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
          selection + " AND " + KEY_SYNC_SEQUENCE_LOCAL + " = 0", selectionArgs, KEY_ROWID);
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
        } catch (RemoteException | SQLiteConstraintException e) {
          syncResult.databaseError = true;
          notifyDatabaseError(e, account);
          return;
        }
      } while (cursor.moveToNext());
    }
    cursor.close();

    try {
      cursor = provider.query(TransactionProvider.ACCOUNTS_URI, projection, selection, selectionArgs,
          KEY_ROWID);
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
          SequenceNumber lastSyncedRemote = SequenceNumber.parse(getUserDataWithDefault(accountManager, account,
              lastRemoteSyncKey, "0"));
          final org.totschnig.myexpenses.model.Account instanceFromDb = org.totschnig.myexpenses.model.Account.getInstanceFromDb(accountId);
          if (instanceFromDb == null) {
            // might have been deleted by user in the meantime
            continue;
          }
          syncDelegate.account = instanceFromDb;
          if (uuidFromExtras != null && extras.getBoolean(KEY_RESET_REMOTE_ACCOUNT)) {
            try {
              backend.resetAccountData(uuidFromExtras);
              appendToNotification(getContext().getString(R.string.sync_success_reset_account_data,
                  instanceFromDb.getLabel()), account, true);
            } catch (IOException e) {
              log().w(e);
              if (handleAuthException(backend, e, account)) {
                return;
              }
              syncResult.stats.numIoExceptions++;
              syncResult.delayUntil = getIoDefaultDelaySeconds();
              notifyIoException(R.string.sync_io_exception_reset_account_data, account);
            }
            break;
          }
          appendToNotification(getContext().getString(R.string.synchronization_start, instanceFromDb.getLabel()), account, true);
          try {
            backend.withAccount(instanceFromDb);
          } catch (IOException e) {
            log().w(e);
            if (handleAuthException(backend, e, account)) {
              return;
            }
            syncResult.stats.numIoExceptions++;
            syncResult.delayUntil = getIoDefaultDelaySeconds();
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
            syncResult.delayUntil = getIoLockDelaySeconds();
            continue;
          }

          boolean completedWithoutError = false;
          int successRemote2Local = 0, successLocal2Remote = 0;
          try {
            Optional<ChangeSet> changeSetSince = backend.getChangeSetSince(lastSyncedRemote, getContext());

            List<TransactionChange> remoteChanges;
            if (changeSetSince.isPresent()) {
              lastSyncedRemote = changeSetSince.get().sequenceNumber;
              remoteChanges = changeSetSince.get().changes;
            } else {
              remoteChanges = new ArrayList<>();
            }

            List<TransactionChange> localChanges = new ArrayList<>();

            long sequenceToTest = lastSyncedLocal;

            while (true) {
              sequenceToTest++;
              List<TransactionChange> nextChanges = getLocalChanges(provider, accountId, sequenceToTest);
              if (nextChanges.size() > 0) {
                localChanges.addAll(nextChanges);
                lastSyncedLocal = sequenceToTest;
              } else {
                break;
              }
            }

            if (localChanges.size() > 0 || remoteChanges.size() > 0) {

              TransactionChange localMetadataChange = syncDelegate.findMetadataChange(localChanges);
              TransactionChange remoteMetadataChange = syncDelegate.findMetadataChange(remoteChanges);
              if (remoteMetadataChange != null) {
                remoteChanges = syncDelegate.removeMetadataChange(remoteChanges);
              }

              if (localMetadataChange != null && remoteMetadataChange != null) {
                if (localMetadataChange.timeStamp() > remoteMetadataChange.timeStamp()) {
                  remoteMetadataChange = null;
                } else {
                  localMetadataChange = null;
                  localChanges = syncDelegate.removeMetadataChange(localChanges);
                }
              }

              if (localMetadataChange != null) {
                backend.updateAccount(instanceFromDb);
              } else if (remoteMetadataChange != null) {
                final Exceptional<AccountMetaData> accountMetaDataExceptional = backend.readAccountMetaData();
                if (accountMetaDataExceptional.isPresent()) {
                  if (updateAccountFromMetadata(provider, accountMetaDataExceptional.get())) {
                    successRemote2Local += 1;
                  } else {
                    appendToNotification("Error while writing account metadata to database: " + accountMetaDataExceptional.getException().getMessage(), account, false);
                  }
                }
              }

              if (localChanges.size() > 0) {
                localChanges = syncDelegate.collectSplits(localChanges);
              }

              Pair<List<TransactionChange>, List<TransactionChange>> mergeResult =
                  syncDelegate.mergeChangeSets(localChanges, remoteChanges);
              localChanges = mergeResult.first;
              remoteChanges = mergeResult.second;

              if (remoteChanges.size() > 0) {
                syncDelegate.writeRemoteChangesToDb(provider, remoteChanges);
                accountManager.setUserData(account, lastRemoteSyncKey, String.valueOf(lastSyncedRemote));
                successRemote2Local += remoteChanges.size();
              }

              if (localChanges.size() > 0) {
                lastSyncedRemote = backend.writeChangeSet(lastSyncedRemote, localChanges, getContext());
                accountManager.setUserData(account, lastLocalSyncKey, String.valueOf(lastSyncedLocal));
                accountManager.setUserData(account, lastRemoteSyncKey, String.valueOf(lastSyncedRemote));
                successLocal2Remote = localChanges.size();
              }
              if (!BuildConfig.DEBUG) {
                // on debug build for auditing purposes, we keep changes in the table
                provider.delete(TransactionProvider.CHANGES_URI,
                    KEY_ACCOUNTID + " = ? AND " + KEY_SYNC_SEQUENCE_LOCAL + " <= ?",
                    new String[]{String.valueOf(accountId), String.valueOf(lastSyncedLocal)});
              }
            }
            completedWithoutError = true;
          } catch (IOException e) {
            log().w(e);
            if (handleAuthException(backend, e, account)) {
              return;
            }
            syncResult.stats.numIoExceptions++;
            syncResult.delayUntil = getIoDefaultDelaySeconds();
            notifyIoException(R.string.sync_io_exception_syncing, account);
          } catch (RemoteException | OperationApplicationException | SQLiteException e) {
            syncResult.databaseError = true;
            notifyDatabaseError(e, account);
          } catch (Exception e) {
            appendToNotification(String.format("ERROR (%s): %s ", e.getClass().getSimpleName(), e.getMessage()),
                account, true);
            report(e);
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
                syncResult.delayUntil = getIoLockDelaySeconds();
              }
            }
          }
        } while (cursor.moveToNext());
      }
      cursor.close();
    }
    backend.tearDown();
  }

  private CurrencyContext getCurrencyConext() {
    return ((MyApplication) getContext().getApplicationContext()).getAppComponent().currencyContext();
  }

  private boolean updateAccountFromMetadata(ContentProviderClient provider, AccountMetaData accountMetaData) throws RemoteException, OperationApplicationException {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ops.add(TransactionProvider.pauseChangeTrigger());
    ContentValues values = new ContentValues();
    values.put(KEY_LABEL, accountMetaData.label());
    values.put(KEY_OPENING_BALANCE, accountMetaData.openingBalance());
    values.put(KEY_DESCRIPTION, accountMetaData.description());
    final String currency = accountMetaData.currency();
    values.put(KEY_CURRENCY, currency);
    values.put(KEY_TYPE, accountMetaData.type());
    values.put(KEY_COLOR, accountMetaData.color());
    values.put(KEY_EXCLUDE_FROM_TOTALS, accountMetaData._excludeFromTotals());
    if (accountMetaData._criterion() != 0L) {
      values.put(KEY_CRITERION, accountMetaData._criterion());
    }
    final long id = syncDelegate.account.getId();
    ops.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, id)).withValues(values).build());
    String homeCurrency = PrefKey.HOME_CURRENCY.getString(null);
    final Double exchangeRate = accountMetaData.exchangeRate();
    if (exchangeRate != null && homeCurrency != null && homeCurrency.equals(accountMetaData.exchangeRateOtherCurrency())) {
      Uri uri = ContentUris.appendId(TransactionProvider.ACCOUNT_EXCHANGE_RATE_URI.buildUpon(), id)
          .appendEncodedPath(currency)
          .appendEncodedPath(homeCurrency).build();
      int minorUnitDelta = Utils.getHomeCurrency().getFractionDigits() - getCurrencyConext().get(currency).getFractionDigits();
      ops.add(ContentProviderOperation.newInsert(uri).withValue(KEY_EXCHANGE_RATE, exchangeRate * Math.pow(10, minorUnitDelta)).build());
    }
    ops.add(TransactionProvider.resumeChangeTrigger());
    ContentProviderResult[] contentProviderResults = provider.applyBatch(ops);
    int opsSize = ops.size();
    int resultsSize = contentProviderResults.length;
    if (opsSize != resultsSize) {
      CrashHandler.reportWithTag(String.format(Locale.ROOT, "applied %d operations, received %d results",
          opsSize, resultsSize), TAG);
      return false;
    }
    return true;
  }

  private void handleAutoBackupSync(Account account, ContentProviderClient provider, SyncBackendProvider backend) {
    String autoBackupFileUri = getStringSetting(provider, KEY_UPLOAD_AUTO_BACKUP_URI);
    if (autoBackupFileUri != null) {
      String autoBackupCloud = getStringSetting(provider, PrefKey.AUTO_BACKUP_CLOUD.getKey());
      if (autoBackupCloud != null && autoBackupCloud.equals(account.name)) {
        String fileName = getStringSetting(provider, KEY_UPLOAD_AUTO_BACKUP_NAME);
        try {
          if (fileName == null) {
            CrashHandler.report("KEY_UPLOAD_AUTO_BACKUP_NAME empty");
            fileName = "backup-" + new SimpleDateFormat("yyyMMdd", Locale.US).format(new Date());
          }
          log().i("Storing backup %s (%s)", fileName, autoBackupFileUri);
          backend.storeBackup(Uri.parse(autoBackupFileUri), fileName);
          removeSetting(provider, KEY_UPLOAD_AUTO_BACKUP_URI);
          removeSetting(provider, KEY_UPLOAD_AUTO_BACKUP_NAME);
          maybeNotifyUser(getContext().getString(R.string.pref_auto_backup_title),
              getContext().getString(R.string.auto_backup_cloud_success, fileName, account.name), null);
        } catch (Exception e) {
          report(e);
          if (!handleAuthException(backend, e, account)) {
            notifyUser(getContext().getString(R.string.pref_auto_backup_title),
                getContext().getString(R.string.auto_backup_cloud_failure, fileName, account.name)
                    + " " + e.getMessage(), null, null);
          }
        }
      }
    }
  }

  private boolean handleAuthException(SyncBackendProvider backend, Exception e, Account account) {
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
      List<StringBuilder> contentBuilders = notificationContent.get(account.hashCode());
      final StringBuilder contentBuilder;
      if (contentBuilders.size() == 0 || newLine) {
        contentBuilder = new StringBuilder();
        contentBuilders.add(0, contentBuilder);
      } else {
        contentBuilder = contentBuilders.get(0);
      }
      if (contentBuilder.length() > 0) {
        contentBuilder.append(" ");
      }
      contentBuilder.append(content);
      notifyUser(getNotificationTitle(),
          syncDelegate.concat(contentBuilders),
          account, null);
    }
  }

  public static Timber.Tree log() {
    return Timber.tag(TAG);
  }

  private void report(Throwable e) {
    CrashHandler.report(e, TAG);
  }

  private void maybeNotifyUser(String title, String content, @Nullable Account account) {
    if (shouldNotify) {
      notifyUser(title, content, account, null);
    }
  }

  private void notifyUser(String title, CharSequence content, @Nullable Account account, @Nullable Intent intent) {
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

  private void notifyWithResolution(SyncBackendProvider.ResolvableSetupException exception) {
    final PendingIntent resolution = exception.getResolution();
    if (resolution != null) {
      NotificationBuilderWrapper builder = NotificationBuilderWrapper.bigTextStyleBuilder(
          getContext(), NotificationBuilderWrapper.CHANNEL_ID_SYNC, getNotificationTitle(), exception.getMessage());
      builder.setContentIntent(resolution);
      Notification notification = builder.build();
      notification.flags = Notification.FLAG_AUTO_CANCEL;
      ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(
          "SYNC", 0, notification);
    }
  }

  private void notifyIoException(int resId, Account account) {
    appendToNotification(getContext().getString(resId), account, true);
  }

  private void notifyDatabaseError(Exception e, Account account) {
    report(e);
    appendToNotification(getContext().getString(R.string.sync_database_error) + " " + e.getMessage(),
        account, true);
  }

  private String getNotificationTitle() {
    return TextUtils.concatResStrings(getContext(), " ", R.string.app_name, R.string.synchronization);
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
      provider.update(TransactionProvider.ACCOUNTS_URI.buildUpon()
              .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_CALLER_IS_SYNCADAPTER, "1").build(), currentSyncIncrease, KEY_ROWID + " = ? AND " + KEY_SYNC_SEQUENCE_LOCAL + " < ?",
          new String[]{String.valueOf(accountId), String.valueOf(nextSequence)});
    }
    if (hasLocalChanges) {
      Cursor c = provider.query(changesUri, null, null, null, null);
      if (c != null) {
        if (c.moveToFirst()) {
          do {
            TransactionChange transactionChange = TransactionChange.create(c);
            if (!transactionChange.isEmpty()) {
              if (transactionChange.type() == TransactionChange.Type.created || transactionChange.type() == TransactionChange.Type.updated) {
                Cursor tagCursor = provider.query(TransactionProvider.TRANSACTIONS_TAGS_URI, null,
                    String.format("%s = (SELECT %s FROM %s WHERE %s = ?)", KEY_TRANSACTIONID, KEY_ROWID, TABLE_TRANSACTIONS, KEY_UUID),
                    new String[]{transactionChange.uuid()}, null);
                if (tagCursor != null) {
                  if (tagCursor.moveToFirst()) {
                    List<String> tags = new ArrayList<>();
                    do {
                      tags.add(tagCursor.getString(tagCursor.getColumnIndex(DatabaseConstants.KEY_LABEL)));
                    } while (tagCursor.moveToNext());
                    transactionChange = transactionChange.toBuilder().setTags(tags).build();
                  }
                  tagCursor.close();
                }
              }
              result.add(transactionChange);
            }
          } while (c.moveToNext());
        }
        c.close();
      }
    }
    return result;
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

  private boolean isConnectedWifi(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) return false;
    NetworkInfo info = cm.getActiveNetworkInfo();
    return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
  }

  private boolean getBooleanSetting(ContentProviderClient provider, PrefKey prefKey, boolean defaultValue) {
    String value = getStringSetting(provider, prefKey.getKey());
    return value != null ? value.equals(Boolean.TRUE.toString()) : defaultValue;
  }

  private String getStringSetting(ContentProviderClient provider, String prefKey) {
    String result = null;
    try {
      Cursor cursor = provider.query(TransactionProvider.SETTINGS_URI, new String[]{KEY_VALUE},
          KEY_KEY + " = ?", new String[]{prefKey}, null);
      if (cursor != null) {
        if (cursor.moveToFirst()) {
          result = cursor.getString(0);
        }
        cursor.close();
      }
    } catch (RemoteException ignored) {
    }
    return result;
  }

  private void removeSetting(ContentProviderClient provider, String prefKey) {
    try {
      provider.delete(TransactionProvider.SETTINGS_URI, KEY_KEY + " = ?", new String[]{prefKey});
    } catch (RemoteException ignored) {
    }
  }
}
