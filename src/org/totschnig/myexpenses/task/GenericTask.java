package org.totschnig.myexpenses.task;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.ZipUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * 
 * Note that we need to check if the callbacks are null in each method in case
 * they are invoked after the Activity's and Fragment's onDestroy() method
 * have been called.
 */
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
    Transaction t;
    Long transactionId;
    Long[][] extraInfo2d;
    ContentResolver cr;
    int successCount = 0;
    switch (mTaskId) {
    case TaskExecutionFragment.TASK_CLONE:
      for (long id : (Long[]) ids) {
        t = Transaction.getInstanceFromDb(id);
        if (t != null && t.saveAsNew() != null)
          successCount++;
      }
      return successCount;
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2:
      t = Transaction.getInstanceFromDb((Long) ids[0]);
      if (t != null && t instanceof SplitTransaction)
        ((SplitTransaction) t).prepareForEdit();
      return t;
    case TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE:
      return Template.getInstanceFromDb((Long) ids[0]);
    case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
      // when we are called from a notification,
      // the template could have been deleted in the meantime
      // getInstanceFromTemplate should return null in that case
      return Transaction.getInstanceFromTemplate((Long) ids[0]);
    case TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE:
      for (int i = 0; i < ids.length; i++) {
        t = Transaction.getInstanceFromTemplate((Long) ids[i]);
        if (t != null) {
          if (mExtra != null) {
            extraInfo2d = (Long[][]) mExtra;
            t.setDate(new Date(extraInfo2d[i][1]));
            t.originPlanInstanceId = extraInfo2d[i][0];
          }
          if (t.save() != null) {
            successCount++;
          }
        }
      }
      return successCount;
    case TaskExecutionFragment.TASK_REQUIRE_ACCOUNT:
      Account account;
      account = Account.getInstanceFromDb(0);
      if (account == null) {
        account = new Account(MyApplication.getInstance().getString(R.string.app_name), 0,
            MyApplication.getInstance().getString(R.string.default_account_description));
        account.save();
      }
      return account;
    case TaskExecutionFragment.TASK_DELETE_TRANSACTION:
      for (long id : (Long[]) ids) {
        Transaction.delete(id);
      }
      return null;
    case TaskExecutionFragment.TASK_DELETE_ACCOUNT:
      Account.delete((Long) ids[0]);
      return null;
    case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS:
      for (long id : (Long[])ids) {
        PaymentMethod.delete(id);
      }
      return null;
    case TaskExecutionFragment.TASK_DELETE_PAYEES:
      for (long id : (Long[])ids) {
        Payee.delete(id);
      }
      return null;
    case TaskExecutionFragment.TASK_DELETE_CATEGORY:
      for (long id : (Long[])ids) {
        Category.delete(id);
      }
      return null;
    case TaskExecutionFragment.TASK_DELETE_TEMPLATES:
      for (long id : (Long[]) ids) {
        Template.delete(id);
      }
      return null;
    case TaskExecutionFragment.TASK_TOGGLE_CRSTATUS:
      cr = MyApplication.getInstance().getContentResolver();
      cr.update(
          TransactionProvider.TRANSACTIONS_URI
            .buildUpon()
            .appendPath(String.valueOf(ids[0]))
            .appendPath(TransactionProvider.URI_SEGMENT_TOGGLE_CRSTATUS)
            .build(),
          null, null, null);
      return null;
    case TaskExecutionFragment.TASK_MOVE:
      Transaction.move((Long) ids[0], (Long) mExtra);
      return null;
    case TaskExecutionFragment.TASK_NEW_PLAN:
      Uri uri = ((Plan) mExtra).save();
      return uri == null ? null : ContentUris.parseId(uri);
    case TaskExecutionFragment.TASK_NEW_CALENDAR:
      return MyApplication.getInstance().createPlanner();
    case TaskExecutionFragment.TASK_CANCEL_PLAN_INSTANCE:
      cr = MyApplication.getInstance().getContentResolver();
      for (int i = 0; i < ids.length; i++) {
        extraInfo2d = (Long[][]) mExtra;
        transactionId = extraInfo2d[i][1];
        Long templateId = extraInfo2d[i][0];
        if (transactionId != null && transactionId > 0L) {
          Transaction.delete(transactionId);
        } else {
          cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
              KEY_INSTANCEID + " = ?",
              new String[] { String.valueOf(ids[i]) });
        }
        ContentValues values = new ContentValues();
        values.putNull(KEY_TRANSACTIONID);
        values.put(KEY_TEMPLATEID, templateId);
        values.put(KEY_INSTANCEID, (Long) ids[i]);
        cr.insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
      }
      return null;
    case TaskExecutionFragment.TASK_RESET_PLAN_INSTANCE:
      cr = MyApplication.getInstance().getContentResolver();
      for (int i = 0; i < ids.length; i++) {
        transactionId = ((Long[]) mExtra)[i];
        if (transactionId != null && transactionId > 0L) {
          Transaction.delete(transactionId);
        }
        cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
            KEY_INSTANCEID + " = ?", new String[] { String.valueOf(ids[i]) });
      }
      return null;
    case TaskExecutionFragment.TASK_BACKUP:
      boolean result = false;
      File backupFile = (File) mExtra;
      File cacheDir = Utils.getCacheDir();
      if (cacheDir == null) {
        return new Result(false,R.string.external_storage_unavailable);
      }
      if (MyApplication.getInstance().backup(cacheDir)) {
        result = ZipUtils.zip(cacheDir.listFiles(),backupFile);
        MyApplication.getBackupDbFile(cacheDir).delete();
        MyApplication.getBackupPrefFile(cacheDir).delete();
      }
      return new Result(result,result ? R.string.backup_success : R.string.backup_failure);
    case TaskExecutionFragment.TASK_BALANCE:
      Account.getInstanceFromDb((Long) ids[0]).balance((Boolean) mExtra);
      return null;
    case TaskExecutionFragment.TASK_UPDATE_SORT_KEY:
      cr = MyApplication.getInstance().getContentResolver();
      ContentValues values = new ContentValues();
      values.put(DatabaseConstants.KEY_SORT_KEY, (Integer) mExtra);
      cr.update(
          TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(ids [0])).build(),
          values,null,null);
      return null;
    case TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS:
      SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings().edit()
          .putInt(((String) (ids[0]))+Money.KEY_CUSTOM_FRACTION_DIGITS, (Integer) mExtra));
      //TODO handle DB update
      return null;
    }
    return null;
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