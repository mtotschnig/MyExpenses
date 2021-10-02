package org.totschnig.myexpenses.task;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.calendar.CalendarContractCompat;
import com.annimon.stream.Collectors;
import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

/**
 * Note that we need to check if the callbacks are null in each method in case
 * they are invoked after the Activity's and Fragment's onDestroy() method
 * have been called.
 */
@Deprecated
public class GenericTask<T> extends AsyncTask<T, Void, Object> {
  private final TaskExecutionFragment taskExecutionFragment;
  private int mTaskId;
  private Serializable mExtra;

  public GenericTask(TaskExecutionFragment taskExecutionFragment, int taskId, Serializable extra) {
    this.taskExecutionFragment = taskExecutionFragment;

    mTaskId = taskId;
    mExtra = extra;
  }

  @Override
  protected void onPreExecute() {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPreExecute();
    }
  }

  /**
   * Note that we do NOT call the callback object's methods directly from the
   * background thread, as this could result in a race condition.
   */
  @Override
  protected Object doInBackground(T... ids) {
    Long transactionId;
    Long[][] extraInfo2d;
    final MyApplication application = MyApplication.getInstance();
    final Context context = ContextHelper.wrap(application, application.getAppComponent().userLocaleProvider().getUserPreferredLocale());
    ContentResolver cr = context.getContentResolver();
    ContentValues values;
    Cursor c;
    int successCount = 0, failureCount = 0;
    switch (mTaskId) {
      case TaskExecutionFragment.TASK_INSTANTIATE_PLAN:
        return Plan.getInstanceFromDb((Long) ids[0]);
      case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS:
        try {
          for (long id : (Long[]) ids) {
            PaymentMethod.delete(id);
          }
        } catch (SQLiteConstraintException e) {
          CrashHandler.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_TOGGLE_CRSTATUS:
        cr.update(
            TransactionProvider.TRANSACTIONS_URI
                .buildUpon()
                .appendPath(String.valueOf(ids[0]))
                .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
                .build(),
            null, null, null);
        return null;
      case TaskExecutionFragment.TASK_SWAP_SORT_KEY:
        cr.update(
            TransactionProvider.ACCOUNTS_URI
                .buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_SWAP_SORT_KEY)
                .appendPath((String) ids[0])
                .appendPath((String) ids[1])
                .build(),
            null, null, null);
        return null;
      case TaskExecutionFragment.TASK_MOVE:
        Transaction.move((Long) ids[0], (Long) mExtra);
        return null;
      case TaskExecutionFragment.TASK_MOVE_CATEGORY:
        for (long id : (Long[]) ids) {
          if (Category.move(id, (Long) mExtra))
            successCount++;
          else
            failureCount++;
        }
        String resultMsg = "";
        if (successCount > 0) {
          resultMsg += context.getResources().getQuantityString(R.plurals.move_category_success, successCount, successCount);
        }
        if (failureCount > 0) {
          if (!TextUtils.isEmpty(resultMsg)) {
            resultMsg += " ";
          }
          resultMsg += context.getResources().getQuantityString(R.plurals.move_category_failure, failureCount, failureCount);
        }
        return successCount > 0 ? Result.ofSuccess(resultMsg) : Result.ofFailure(resultMsg);
      case TaskExecutionFragment.TASK_BALANCE:
        return Account.getInstanceFromDb((Long) ids[0]).balance((Boolean) mExtra);
      case TaskExecutionFragment.TASK_UPDATE_SORT_KEY:
        values = new ContentValues();
        values.put(DatabaseConstants.KEY_SORT_KEY, (Integer) mExtra);
        cr.update(
            TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(ids[0])).build(),
            values, null, null);
        return null;
      case TaskExecutionFragment.TASK_SET_EXCLUDE_FROM_TOTALS:
        return updateBooleanAccountFieldFromExtra(cr, (Long[]) ids, DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS) ? Result.SUCCESS : Result.FAILURE;
      case TaskExecutionFragment.TASK_SET_ACCOUNT_SEALED:
        return updateBooleanAccountFieldFromExtra(cr, (Long[]) ids, DatabaseConstants.KEY_SEALED) ? Result.SUCCESS : Result.FAILURE;
      case TaskExecutionFragment.TASK_SET_ACCOUNT_HIDDEN:
        return updateBooleanAccountFieldFromExtra(cr, (Long[]) ids, DatabaseConstants.KEY_HIDDEN) ? Result.SUCCESS : Result.FAILURE;
      case TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS: {
        //we need to check if there are transfer parts that refer to the account we try to move to,
        //if yes we cannot move
        transactionId = (Long) ids[0];
        Long accountId = (Long) mExtra;
        boolean success;
        c = cr.query(TransactionProvider.UNCOMMITTED_URI,
            new String[]{"count(*)"},
            DatabaseConstants.KEY_PARENTID + " = ? AND " + DatabaseConstants.KEY_TRANSFER_ACCOUNT + "  = ?",
            new String[]{String.valueOf(transactionId), String.valueOf(accountId)}, null);
        success = (c != null && c.moveToFirst() && c.getInt(0) == 0);
        c.close();
        if (success) {
          values = new ContentValues();
          values.put(DatabaseConstants.KEY_ACCOUNTID, accountId);
          cr.update(TransactionProvider.TRANSACTIONS_URI, values,
              DatabaseConstants.KEY_PARENTID + " = ? AND " + KEY_STATUS + " = " + STATUS_UNCOMMITTED,
              new String[]{String.valueOf(transactionId)});
          return true;
        }
        return false;
      }
      case TaskExecutionFragment.TASK_REPAIR_PLAN:
        String calendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1");
        if (calendarId.equals("-1")) {
          return false;
        }
        values = new ContentValues();
        for (String uuid : (String[]) ids) {
          Cursor eventCursor = cr.query(CalendarContractCompat.Events.CONTENT_URI, new String[]{CalendarContractCompat.Events._ID},
              CalendarContractCompat.Events.CALENDAR_ID + " = ? AND " + CalendarContractCompat.Events.DESCRIPTION
                  + " LIKE ?", new String[]{calendarId,
                  "%" + uuid + "%"}, null);
          if (eventCursor != null) {
            if (eventCursor.moveToFirst()) {
              values.put(KEY_PLANID, eventCursor.getLong(0));
              cr.update(TransactionProvider.TEMPLATES_URI, values,
                  DatabaseConstants.KEY_UUID + " = ?",
                  new String[]{uuid});
            }
            eventCursor.close();
          }
        }
        return true;
      case TaskExecutionFragment.TASK_SYNC_LINK_LOCAL: {
        Account account = Account.getInstanceFromDb(Account.findByUuid((String) ids[0]));
        if (account.isSealed()) {
          return Result.ofFailure(R.string.object_sealed);
        }
        String syncAccountName = (String) this.mExtra;
        account.setSyncAccountName(syncAccountName);
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putString(KEY_UUID, account.getUuid());
        bundle.putBoolean(SyncAdapter.KEY_RESET_REMOTE_ACCOUNT, true);
        ContentResolver.requestSync(GenericAccountService.getAccount(syncAccountName),
            TransactionProvider.AUTHORITY, bundle);
        account.save();
        return Result.SUCCESS;
      }
      case TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND: {
        AccountManagerFuture<Boolean> accountManagerFuture = AccountManager.get(context).removeAccount(
            GenericAccountService.getAccount((String) ids[0]), null, null);
        try {
          return accountManagerFuture.getResult() ? Result.SUCCESS : Result.FAILURE;
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          CrashHandler.report(e);
          return Result.ofFailure(e.getMessage());
        }
      }
      case TaskExecutionFragment.TASK_SYNC_LINK_SAVE: {
        //first get remote data for account
        String syncAccountName = ((String) mExtra);
        Exceptional<SyncBackendProvider> syncBackendProvider = getSyncBackendProviderFromExtra();
        if (!syncBackendProvider.isPresent()) {
          return Result.ofFailure(syncBackendProvider.getException().getMessage());
        }
        List<String> remoteUuidList;
        try {
          Stream<AccountMetaData> remoteAccounStream = syncBackendProvider.get().getRemoteAccountStream()
              .filter(Exceptional::isPresent)
              .map(Exceptional::get);
          remoteUuidList = remoteAccounStream
              .map(AccountMetaData::uuid)
              .collect(Collectors.toList());
        } catch (IOException e) {
          return Result.ofFailure(e.getMessage());
        } finally {
          syncBackendProvider.get().tearDown();
        }
        int requested = ids.length;
        c = cr.query(TransactionProvider.ACCOUNTS_URI,
            new String[]{KEY_ROWID},
            KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(requested) + " AND (" + KEY_UUID + " IS NULL OR NOT " +
                KEY_UUID + " " + WhereFilter.Operation.IN.getOp(remoteUuidList.size()) + ")",
            Stream.concat(
                Stream.of(((Long[]) ids)).map(String::valueOf),
                Stream.of(remoteUuidList))
                .toArray(size -> new String[size]),
            null);
        if (c == null) {
          return Result.ofFailure("Cursor is null");
        }
        int result = 0;
        if (c.moveToFirst()) {
          result = c.getCount();
          while (!c.isAfterLast()) {
            Account account = Account.getInstanceFromDb(c.getLong(0));
            account.setSyncAccountName(syncAccountName);
            account.save();
            c.moveToNext();
          }
        }
        c.close();
        String message = "";
        if (result > 0) {
          message = context.getString(R.string.link_account_success, result);
        }
        if (requested > result) {
          message += " " + context.getString(R.string.link_account_failure_1, requested - result)
              + " " + context.getString(R.string.link_account_failure_2)
              + " " + context.getString(R.string.link_account_failure_3);
        }
        return requested == result ? Result.ofSuccess(message) : Result.ofFailure(message);
      }
      case TaskExecutionFragment.TASK_SETUP_FROM_SYNC_ACCOUNTS: {
        String syncAccountName = (String) mExtra;
        Exceptional<SyncBackendProvider> syncBackendProvider = getSyncBackendProviderFromExtra();
        if (!syncBackendProvider.isPresent()) {
          return Result.ofFailure(syncBackendProvider.getException().getMessage());
        }
        try {
          List<String> accountUuids = Arrays.asList((String[]) ids);
          int numberOfRestoredAccounts = syncBackendProvider.get().getRemoteAccountStream()
              .filter(Exceptional::isPresent)
              .map(Exceptional::get)
              .filter(accountMetaData -> accountUuids.contains(accountMetaData.uuid()))
              .map(accountMetaData -> accountMetaData.toAccount(application.getAppComponent().currencyContext()))
              .mapToInt(account -> {
                account.setSyncAccountName(syncAccountName);
                return account.save() == null ? 0 : 1;
              })
              .sum();
          if (numberOfRestoredAccounts == 0) {
            return Result.FAILURE;
          } else {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(GenericAccountService.getAccount(syncAccountName),
                TransactionProvider.AUTHORITY, bundle);
            return Result.SUCCESS;
          }
        } catch (IOException e) {
          return Result.FAILURE;
        } finally {
          syncBackendProvider.get().tearDown();
        }
      }
      case TaskExecutionFragment.TASK_REPAIR_SYNC_BACKEND: {
        Optional<SyncBackendProviderFactory> syncBackendProviderFactoryOptional =
            Stream.of(ServiceLoader.load(context)).filter(factory -> factory.getLabel().equals(ids[0])).findFirst();
        if (syncBackendProviderFactoryOptional.isPresent()) {
          return syncBackendProviderFactoryOptional.get().handleRepairTask(mExtra);
        } else {
          return Result.FAILURE;
        }
      }
      case TaskExecutionFragment.TASK_CATEGORY_COLOR: {
        return Category.updateColor((Long) ids[0], (Integer) mExtra) ? Result.SUCCESS :
            Result.ofFailure("Error while saving color for category");
      }
    }
    return null;
  }

  private boolean updateBooleanAccountFieldFromExtra(ContentResolver cr, Long[] accountIds, String key) {
    ContentValues values;
    values = new ContentValues();
    values.put(key, (Boolean) mExtra);
    return cr.update(
        TransactionProvider.ACCOUNTS_URI, values,
        String.format("%s %s", KEY_ROWID, WhereFilter.Operation.IN.getOp(accountIds.length)),
        Stream.of(accountIds).map(String::valueOf).toArray(String[]::new)) == accountIds.length;
  }

  @NonNull
  private Exceptional<SyncBackendProvider> getSyncBackendProviderFromExtra() {
    return GenericAccountService.Companion.getSyncBackendProviderLegacy(MyApplication.getInstance(), (String) mExtra);
  }

  private boolean deleteAccount(Long anId) {
    try {
      Account.delete(anId);
    } catch (RemoteException | OperationApplicationException e) {
      CrashHandler.reportWithDbSchema(e);
      return false;
    }
    return true;
  }

  @Override
  protected void onProgressUpdate(Void... ignore) {
    /*
     * if (mCallbacks != null) { mCallbacks.onProgressUpdate(ignore[0]); }
     */
  }

  @Override
  protected void onCancelled() {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onCancelled();
    }
  }

  @Override
  protected void onPostExecute(Object result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(mTaskId, result);
    }
  }
}