package org.totschnig.myexpenses.task;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.MyApplication.PrefKey;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.ZipUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import com.android.calendar.CalendarContractCompat.Events;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

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
    ContentResolver cr = MyApplication.getInstance().getContentResolver();
    ContentValues values;
    Cursor c;
    int successCount = 0;
    switch (mTaskId) {
/*    case TaskExecutionFragment.TASK_CLONE:
      for (long id : (Long[]) ids) {
        t = Transaction.getInstanceFromDb(id);
        if (t!=null) {
          t.crStatus = CrStatus.UNRECONCILED;
          if (t.saveAsNew() != null)
            successCount++;
        }
      }
      return successCount;*/
    case TaskExecutionFragment.TASK_SPLIT:
      //ids could have been passed through bundle to ContribInfoDialog
      //and in bundle looses its type as long array (becomes object array)
      //https://code.google.com/p/android/issues/detail?id=3847
      for (T id : ids) {
        t = Transaction.getInstanceFromDb((Long) id);
        if (t!=null  && !(t instanceof SplitTransaction)) {
          SplitTransaction parent = SplitTransaction.getNewInstance(t.accountId,false);
          parent.amount = t.amount;
          parent.setDate(t.getDate());
          parent.save();
          values = new ContentValues();
          values.put(DatabaseConstants.KEY_PARENTID, parent.getId());
          if (cr.update(
              TransactionProvider.TRANSACTIONS_URI.buildUpon().appendPath(String.valueOf(id)).build(),
              values,null,null)>0) {
            successCount++;
          }
        }
      }
      ContribFeature.SPLIT_TRANSACTION.recordUsage();
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
        account = new Account(MyApplication.getInstance().getString(R.string.default_account_name), 0,
            MyApplication.getInstance().getString(R.string.default_account_description));
        account.save();
      }
      return account;
    case TaskExecutionFragment.TASK_DELETE_TRANSACTION:
      try {
        for (long id : (Long[]) ids) {
          Transaction.delete(id,(boolean)mExtra);
        }
      } catch (SQLiteConstraintException e) {
        Utils.reportToAcraWithDbSchema(e);
        return false;
      }
      return true;
      case TaskExecutionFragment.TASK_UNDELETE_TRANSACTION:
        try {
          for (long id : (Long[]) ids) {
            Transaction.undelete(id);
          }
        } catch (SQLiteConstraintException e) {
          Utils.reportToAcraWithDbSchema(e);
          return false;
        }
        return true;
    case TaskExecutionFragment.TASK_DELETE_ACCOUNT:
      try {
        Account.delete((Long) ids[0]);
      } catch (Exception e) {
        Utils.reportToAcraWithDbSchema(e);
        return false;
      }
      return true;
    case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS:
      try {
        for (long id : (Long[])ids) {
          PaymentMethod.delete(id);
        }
      } catch (SQLiteConstraintException e) {
        Utils.reportToAcraWithDbSchema(e);
        return false;
      }
      return true;
    case TaskExecutionFragment.TASK_DELETE_PAYEES:
      try {
        for (long id : (Long[])ids) {
          Payee.delete(id);
        }
      } catch (SQLiteConstraintException e) {
        Utils.reportToAcraWithDbSchema(e);
        return false;
      }
      return true;
    case TaskExecutionFragment.TASK_DELETE_CATEGORY:
      try {
        for (long id : (Long[])ids) {
          Category.delete(id);
        }
      } catch (SQLiteConstraintException e) {
        Utils.reportToAcraWithDbSchema(e);
        return false;
      }
      return true;
    case TaskExecutionFragment.TASK_DELETE_TEMPLATES:
      try {
        for (long id : (Long[]) ids) {
          Template.delete(id);
        }
      } catch (SQLiteConstraintException e) {
        Utils.reportToAcraWithDbSchema(e);
        return false;
      }
      return true;
    case TaskExecutionFragment.TASK_TOGGLE_CRSTATUS:
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
      for (int i = 0; i < ids.length; i++) {
        extraInfo2d = (Long[][]) mExtra;
        transactionId = extraInfo2d[i][1];
        Long templateId = extraInfo2d[i][0];
        if (transactionId != null && transactionId > 0L) {
          Transaction.delete(transactionId, false);
        } else {
          cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
              KEY_INSTANCEID + " = ?",
              new String[] { String.valueOf(ids[i]) });
        }
        values = new ContentValues();
        values.putNull(KEY_TRANSACTIONID);
        values.put(KEY_TEMPLATEID, templateId);
        values.put(KEY_INSTANCEID, (Long) ids[i]);
        cr.insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
      }
      return null;
    case TaskExecutionFragment.TASK_RESET_PLAN_INSTANCE:
      for (int i = 0; i < ids.length; i++) {
        transactionId = ((Long[]) mExtra)[i];
        if (transactionId != null && transactionId > 0L) {
          Transaction.delete(transactionId, false);
        }
        cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
            KEY_INSTANCEID + " = ?", new String[] { String.valueOf(ids[i]) });
      }
      return null;
    case TaskExecutionFragment.TASK_BACKUP:
      boolean result = false;
      if (!Utils.isExternalStorageAvailable()) {
        return new Result(false,R.string.external_storage_unavailable);
      }
      DocumentFile backupFile = MyApplication.requireBackupFile();
      if (backupFile == null) {
        Utils.reportToAcra(new Exception(
            MyApplication.getInstance().getString(R.string.io_error_appdir_null)));
        return new Result(false,R.string.io_error_appdir_null);
      }
      File cacheDir = Utils.getCacheDir();
      if (cacheDir == null) {
        Utils.reportToAcra(new Exception(
            MyApplication.getInstance().getString(R.string.io_error_cachedir_null)));
        return new Result(false,R.string.io_error_cachedir_null);
      }
      cacheEventData();
      if (MyApplication.getInstance().backup(cacheDir)) {
        try {
          ZipUtils.zipBackup(
              cacheDir,
              backupFile);
          result  = true;
        } catch (IOException e) {
          Utils.reportToAcra(e);
        }
        MyApplication.getBackupDbFile(cacheDir).delete();
        MyApplication.getBackupPrefFile(cacheDir).delete();
      }
      if (result) {
        return new Result(
            true,
            R.string.backup_success,
            backupFile.getUri());
      } else {
        return new Result(
            false,
            R.string.backup_failure,
            FileUtils.getPath(MyApplication.getInstance(), backupFile.getUri()));
      }
    case TaskExecutionFragment.TASK_BALANCE:
      Account.getInstanceFromDb((Long) ids[0]).balance((Boolean) mExtra);
      return null;
    case TaskExecutionFragment.TASK_UPDATE_SORT_KEY:
      values = new ContentValues();
      values.put(DatabaseConstants.KEY_SORT_KEY, (Integer) mExtra);
      cr.update(
          TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(ids [0])).build(),
          values,null,null);
      return null;
    case TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS:
      return cr.update(TransactionProvider.CURRENCIES_URI.buildUpon()
          .appendPath(TransactionProvider.URI_SEGMENT_CHANGE_FRACTION_DIGITS)
          .appendPath((String) ids[0])
          .appendPath(String.valueOf((Integer)mExtra))
          .build(),null,null, null);
    case TaskExecutionFragment.TASK_TOGGLE_EXCLUDE_FROM_TOTALS:
      values = new ContentValues();
      values.put(DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS, (Boolean)mExtra);
      cr.update(
          TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(ids [0])).build(),
          values,null,null);
      return null;
      case TaskExecutionFragment.TASK_DELETE_IMAGES:
        for (long id : (Long[]) ids) {
          Uri staleImageUri = TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(String.valueOf(id)).build();
          c = cr.query(
              staleImageUri,
              null,
              null,null,null);
          if (c==null)
            continue;
          if (c.moveToFirst()) {
            Uri imageFileUri = Uri.parse(c.getString(0));
            if (checkImagePath(imageFileUri.getLastPathSegment())) {
              boolean success = false;
              if (imageFileUri.getScheme().equals("file")) {
                success = new File(imageFileUri.getPath()).delete();
              } else {
                success = cr.delete(imageFileUri, null, null) > 0;
              }
              if (success) {
                Log.d(MyApplication.TAG, "Successfully deleted file " + imageFileUri.toString());
              } else {
                Log.e(MyApplication.TAG,"Unable to delete file "+imageFileUri.toString());
              }
            } else {
              Log.d(MyApplication.TAG, imageFileUri.toString() + " not deleted since it might still be in use");
            }
            cr.delete(staleImageUri,null,null);
          }
          c.close();
        }
        return null;
      case TaskExecutionFragment.TASK_SAVE_IMAGES:
        File staleFileDir = new File(MyApplication.getInstance().getExternalFilesDir(null),"images.old");
        staleFileDir.mkdir();
        if (!staleFileDir.isDirectory()) {
          return null;
        }
        for (long id : (Long[]) ids) {
          Uri staleImageUri = TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(String.valueOf(id)).build();
          c = cr.query(
              staleImageUri,
              null,
              null,null,null);
          if (c==null)
            continue;
          if (c.moveToFirst()) {
            boolean success = false;
            Uri imageFileUri = Uri.parse(c.getString(0));
            if (checkImagePath(imageFileUri.getLastPathSegment())) {
              if (imageFileUri.getScheme().equals("file")) {
                File staleFile = new File(imageFileUri.getPath());
                success = staleFile.renameTo(new File(staleFileDir, staleFile.getName()));
              } else {
                try {
                  if (Utils.copy(imageFileUri, Uri.fromFile(new File(staleFileDir, imageFileUri.getLastPathSegment()))))
                    success = cr.delete(imageFileUri, null, null) > 0;
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
              if (success) {
                Log.d(MyApplication.TAG, "Successfully moved file " + imageFileUri.toString());
              }
            } else {
              success = true; //we do not move the file but remove its uri from the table
              Log.d(MyApplication.TAG, imageFileUri.toString() + " not moved since it might still be in use");
            }
            if (success) {
              cr.delete(staleImageUri,null,null);
            } else {
              Log.e(MyApplication.TAG,"Unable to move file "+imageFileUri.toString());
            }
          }
          c.close();
        }
        return null;
    }
    return null;
  }

  private boolean checkImagePath(String lastPathSegment) {
    boolean result = false;
    Cursor c = MyApplication.getInstance().getContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI,
        new String[]{"count(*)"},
        DatabaseConstants.KEY_PICTURE_URI + " LIKE '%"+lastPathSegment+"'",null,null);
    if (c!=null) {
      if (c.moveToFirst() && c.getInt(0) == 0) {
        result = true;
      }
      c.close();
    }
    return result;
  }

  private void cacheEventData() {
    String plannerCalendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1");
    if (plannerCalendarId.equals("-1")) {
      return;
    }
    ContentValues eventValues = new ContentValues();
    ContentResolver cr = MyApplication.getInstance().getContentResolver();
    //remove old cache
    cr.delete(
        TransactionProvider.EVENT_CACHE_URI, null, null);

    Cursor planCursor = cr.query(Template.CONTENT_URI, new String[]{
            DatabaseConstants.KEY_PLANID},
        DatabaseConstants.KEY_PLANID + " IS NOT null", null, null);
    if (planCursor != null) {
      if (planCursor.moveToFirst()) {
        String[] projection = MyApplication.buildEventProjection();
        do {
          long planId = planCursor.getLong(0);
          Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI,
              planId);

          Cursor eventCursor = cr.query(eventUri, projection,
              Events.CALENDAR_ID + " = ?", new String[]{plannerCalendarId}, null);
          if (eventCursor != null) {
            if (eventCursor.moveToFirst()) {
              MyApplication.copyEventData(eventCursor, eventValues);
              cr.insert(TransactionProvider.EVENT_CACHE_URI, eventValues);
            }
            eventCursor.close();
          }
        } while (planCursor.moveToNext());
      }
      planCursor.close();
    }
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