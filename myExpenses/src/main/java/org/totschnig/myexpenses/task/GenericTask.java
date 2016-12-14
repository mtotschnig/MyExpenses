package org.totschnig.myexpenses.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import com.android.calendar.CalendarContractCompat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.FileCopyUtils;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Date;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;

/**
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
    int successCount = 0, failureCount = 0;
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
          if (t != null && !(t instanceof SplitTransaction)) {
            SplitTransaction parent = SplitTransaction.getNewInstance(t.accountId, false);
            parent.setAmount(t.getAmount());
            parent.setDate(t.getDate());
            parent.payeeId = t.payeeId;
            parent.crStatus = t.crStatus;
            parent.save();
            values = new ContentValues();
            values.put(DatabaseConstants.KEY_PARENTID, parent.getId());
            values.put(DatabaseConstants.KEY_CR_STATUS, Transaction.CrStatus.UNRECONCILED.name());
            values.putNull(DatabaseConstants.KEY_PAYEEID);
            if (cr.update(
                TransactionProvider.TRANSACTIONS_URI.buildUpon().appendPath(String.valueOf(id)).build(),
                values, null, null) > 0) {
              successCount++;
            }
          }
        }
        ContribFeature.SPLIT_TRANSACTION.recordUsage();
        return successCount;
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION:
        t = Transaction.getInstanceFromDb((Long) ids[0]);
        if (t != null && t instanceof SplitTransaction)
          ((SplitTransaction) t).prepareForEdit((Boolean) mExtra);
        return t;
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2:
        return Transaction.getInstanceFromDb((Long) ids[0]);
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
      case TaskExecutionFragment.TASK_INSTANTIATE_PLAN:
        return Plan.getInstanceFromDb((Long) ids[0]);
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
            Transaction.delete(id, (boolean) mExtra);
          }
        } catch (SQLiteConstraintException e) {
          AcraHelper.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_UNDELETE_TRANSACTION:
        try {
          for (long id : (Long[]) ids) {
            Transaction.undelete(id);
          }
        } catch (SQLiteConstraintException e) {
          AcraHelper.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_DELETE_ACCOUNT:
        Long anId = (Long) ids[0];
        try {
          Account.delete(anId);
        } catch (Exception e) {
          AcraHelper.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return new Result(true, 0, anId);
      case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS:
        try {
          for (long id : (Long[]) ids) {
            PaymentMethod.delete(id);
          }
        } catch (SQLiteConstraintException e) {
          AcraHelper.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_DELETE_PAYEES:
        try {
          for (long id : (Long[]) ids) {
            Payee.delete(id);
          }
        } catch (SQLiteConstraintException e) {
          AcraHelper.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_DELETE_CATEGORY:
        try {
          for (long id : (Long[]) ids) {
            Category.delete(id);
          }
        } catch (SQLiteConstraintException e) {
          AcraHelper.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_DELETE_TEMPLATES:
        try {
          for (long id : (Long[]) ids) {
            Template.delete(id);
          }
        } catch (SQLiteConstraintException e) {
          AcraHelper.reportWithDbSchema(e);
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
          resultMsg += MyApplication.getInstance().getResources().getQuantityString(R.plurals.move_category_success, successCount, successCount);
        }
        if (failureCount > 0) {
          if (!TextUtils.isEmpty(resultMsg)) {
            resultMsg += " ";
          }
          resultMsg += MyApplication.getInstance().getResources().getQuantityString(R.plurals.move_category_failure, failureCount, failureCount);
        }
        return new Result(successCount > 0, resultMsg);
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
                new String[]{String.valueOf(ids[i])});
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
              KEY_INSTANCEID + " = ?", new String[]{String.valueOf(ids[i])});
        }
        return null;
      case TaskExecutionFragment.TASK_BACKUP:
        return doBackup();
      case TaskExecutionFragment.TASK_BALANCE:
        Account.getInstanceFromDb((Long) ids[0]).balance((Boolean) mExtra);
        return null;
      case TaskExecutionFragment.TASK_UPDATE_SORT_KEY:
        values = new ContentValues();
        values.put(DatabaseConstants.KEY_SORT_KEY, (Integer) mExtra);
        cr.update(
            TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(ids[0])).build(),
            values, null, null);
        return null;
      case TaskExecutionFragment.TASK_CHANGE_FRACTION_DIGITS:
        return cr.update(TransactionProvider.CURRENCIES_URI.buildUpon()
            .appendPath(TransactionProvider.URI_SEGMENT_CHANGE_FRACTION_DIGITS)
            .appendPath((String) ids[0])
            .appendPath(String.valueOf((Integer) mExtra))
            .build(), null, null, null);
      case TaskExecutionFragment.TASK_TOGGLE_EXCLUDE_FROM_TOTALS:
        values = new ContentValues();
        values.put(DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS, (Boolean) mExtra);
        cr.update(
            TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(ids[0])).build(),
            values, null, null);
        return null;
      case TaskExecutionFragment.TASK_DELETE_IMAGES:
        for (long id : (Long[]) ids) {
          Uri staleImageUri = TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(String.valueOf(id)).build();
          c = cr.query(
              staleImageUri,
              null,
              null, null, null);
          if (c == null)
            continue;
          if (c.moveToFirst()) {
            Uri imageFileUri = Uri.parse(c.getString(0));
            if (checkImagePath(imageFileUri.getLastPathSegment())) {
              boolean success;
              if (imageFileUri.getScheme().equals("file")) {
                success = new File(imageFileUri.getPath()).delete();
              } else {
                success = cr.delete(imageFileUri, null, null) > 0;
              }
              if (success) {
                Log.d(MyApplication.TAG, "Successfully deleted file " + imageFileUri.toString());
              } else {
                Log.e(MyApplication.TAG, "Unable to delete file " + imageFileUri.toString());
              }
            } else {
              Log.d(MyApplication.TAG, imageFileUri.toString() + " not deleted since it might still be in use");
            }
            cr.delete(staleImageUri, null, null);
          }
          c.close();
        }
        return null;
      case TaskExecutionFragment.TASK_SAVE_IMAGES:
        File staleFileDir = new File(MyApplication.getInstance().getExternalFilesDir(null), "images.old");
        staleFileDir.mkdir();
        if (!staleFileDir.isDirectory()) {
          return null;
        }
        for (long id : (Long[]) ids) {
          Uri staleImageUri = TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(String.valueOf(id)).build();
          c = cr.query(
              staleImageUri,
              null,
              null, null, null);
          if (c == null)
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
                  FileCopyUtils.copy(imageFileUri, Uri.fromFile(new File(staleFileDir, imageFileUri.getLastPathSegment())));
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
              cr.delete(staleImageUri, null, null);
            } else {
              Log.e(MyApplication.TAG, "Unable to move file " + imageFileUri.toString());
            }
          }
          c.close();
        }
        return null;
      case TaskExecutionFragment.TASK_EXPORT_CATEGRIES:
        DocumentFile appDir = Utils.getAppDir();
        String fullLabel =
            " CASE WHEN " +
                KEY_PARENTID +
                " THEN " +
                "(SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " parent WHERE parent." + KEY_ROWID + " = " + TABLE_CATEGORIES + "." + KEY_PARENTID + ")" +
                " || ':' || " + KEY_LABEL +
                " ELSE " + KEY_LABEL +
                " END";
        //sort sub categories immediately after their main category
        String sort = "CASE WHEN parent_id then parent_id else _id END,parent_id";
        if (appDir == null) {
          return new Result(false, R.string.external_storage_unavailable);
        }
        String fileName = "categories";
        DocumentFile outputFile = Utils.timeStampedFile(
            appDir,
            fileName,
            "text/qif", false);
        if (outputFile == null) {
          return new Result(
              false,
              R.string.io_error_unable_to_create_file,
              fileName,
              FileUtils.getPath(MyApplication.getInstance(), appDir.getUri()));
        }
        try {
          OutputStreamWriter out = new OutputStreamWriter(
              cr.openOutputStream(outputFile.getUri()),
              ((String) mExtra));
          c = cr.query(
              Category.CONTENT_URI,
              new String[]{fullLabel},
              null, null, sort);
          if (c.getCount() == 0) {
            c.close();
            outputFile.delete();
            return new Result(false, R.string.no_categories);
          }
          out.write("!Type:Cat");
          c.moveToFirst();
          while (c.getPosition() < c.getCount()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nN")
                .append(c.getString(0))
                .append("\n^");
            out.write(sb.toString());
            c.moveToNext();
          }
          c.close();
          out.close();
          return new Result(true, R.string.export_sdcard_success,
              outputFile.getUri());
        } catch (IOException e) {
          return new Result(false, R.string.export_sdcard_failure,
              appDir.getName(), e.getMessage());
        }
      case TaskExecutionFragment.TASK_MOVE_UNCOMMITED_SPLIT_PARTS:
        //we need to check if there are transfer parts that refer to the account we try to move to,
        //if yes we cannot move
        transactionId = (Long) ids[0];
        Long accountId = (Long) mExtra;
        boolean success;
        c = cr.query(TransactionProvider.UNCOMMITTED_URI,
            new String[]{"count(*)"},
            DatabaseConstants.KEY_PARENTID + " = ? AND " + DatabaseConstants.KEY_TRANSFER_ACCOUNT + "  = ?",
            new String[]{String.valueOf(transactionId),String.valueOf(accountId)}, null);
        success = (c != null && c.moveToFirst() && c.getInt(0) == 0);
        c.close();
        if (success) {
          values = new ContentValues();
          values.put(DatabaseConstants.KEY_ACCOUNTID, accountId);
          cr.update(TransactionProvider.TRANSACTIONS_URI,values,
              DatabaseConstants.KEY_PARENTID + " = ? AND " + KEY_STATUS + " = " + STATUS_UNCOMMITTED,
              new String[]{String.valueOf(transactionId)});
          return true;
        }
        return false;
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
      case TaskExecutionFragment.TASK_START_SYNC:
        values = new ContentValues();
        values.put(DatabaseConstants.KEY_SYNC_URI, "debug");
        accountId = (Long) ids[0];
        cr.update(
            TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(accountId)).build(),
            values, null, null);
        //TODO if we fail, we should report to user and to ACRA
        MyApplication.createSyncAccount(accountId);
        return null;
    }
    return null;
  }

  @NonNull
  public static Result doBackup() {
    if (!Utils.isExternalStorageAvailable()) {
      return new Result(false, R.string.external_storage_unavailable);
    }
    DocumentFile appDir = Utils.getAppDir();
    if (appDir == null) {
      return new Result(false, R.string.io_error_appdir_null);
    }
    if (!Utils.dirExistsAndIsWritable(appDir)) {
      return new Result(false, R.string.app_dir_not_accessible,
          FileUtils.getPath(MyApplication.getInstance(), appDir.getUri()));
    }
    DocumentFile backupFile = MyApplication.requireBackupFile(appDir);
    if (backupFile == null) {
      return new Result(false, R.string.io_error_backupdir_null);
    }
    File cacheDir = Utils.getCacheDir();
    if (cacheDir == null) {
      AcraHelper.report(new Exception(
          MyApplication.getInstance().getString(R.string.io_error_cachedir_null)));
      return new Result(false, R.string.io_error_cachedir_null);
    }
    Result result = DbUtils.backup(cacheDir);
    String failureMessage = MyApplication.getInstance().getString(R.string.backup_failure,
        FileUtils.getPath(MyApplication.getInstance(), backupFile.getUri()));
    if (result.success) {
      try {
        ZipUtils.zipBackup(
            cacheDir,
            backupFile);
        return new Result(
            true,
            R.string.backup_success,
            backupFile.getUri());
      } catch (IOException e) {
        AcraHelper.report(e);
        return new Result(
            false,
            failureMessage + " " + e.getMessage());
      } finally {
        MyApplication.getBackupDbFile(cacheDir).delete();
        MyApplication.getBackupPrefFile(cacheDir).delete();
      }
    }
    return new Result(
        false,
        failureMessage + " " + result.print(MyApplication.getInstance()));
  }

  private boolean checkImagePath(String lastPathSegment) {
    boolean result = false;
    Cursor c = MyApplication.getInstance().getContentResolver().query(
        TransactionProvider.TRANSACTIONS_URI,
        new String[]{"count(*)"},
        DatabaseConstants.KEY_PICTURE_URI + " LIKE '%" + lastPathSegment + "'", null, null);
    if (c != null) {
      if (c.moveToFirst() && c.getInt(0) == 0) {
        result = true;
      }
      c.close();
    }
    return result;
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