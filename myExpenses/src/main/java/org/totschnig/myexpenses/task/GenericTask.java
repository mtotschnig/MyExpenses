package org.totschnig.myexpenses.task;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;

import com.android.calendar.CalendarContractCompat;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.Serializable;

/**
 * Note that we need to check if the callbacks are null in each method in case
 * they are invoked after the Activity's and Fragment's onDestroy() method
 * have been called.
 */
@Deprecated
public class GenericTask<T> extends AsyncTask<T, Void, Object> {
  private final TaskExecutionFragment taskExecutionFragment;
  private final int mTaskId;
  private final Serializable mExtra;

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