package org.totschnig.myexpenses.task;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;

import com.android.calendar.CalendarContractCompat;
import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

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
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.BackupUtils;
import org.totschnig.myexpenses.util.FileCopyUtils;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;

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
    Transaction t;
    Long transactionId;
    Long[][] extraInfo2d;
    MyApplication application = MyApplication.getInstance();
    ContentResolver cr = application.getContentResolver();
    ContentValues values;
    Cursor c;
    int successCount = 0, failureCount = 0;
    switch (mTaskId) {
      case TaskExecutionFragment.TASK_SPLIT:
        //ids could have been passed through bundle to ContribInfoDialog
        //and in bundle looses its type as long array (becomes object array)
        //https://code.google.com/p/android/issues/detail?id=3847
        for (T id : ids) {
          t = Transaction.getInstanceFromDb((Long) id);
          if (t != null && !(t instanceof SplitTransaction)) {
            SplitTransaction parent = SplitTransaction.getNewInstance(t.getAccountId(), false);
            parent.setAmount(t.getAmount());
            parent.setDate(t.getDate());
            parent.setPayeeId(t.getPayeeId());
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
        if (t != null)
          t.prepareForEdit((Boolean) mExtra);
        return t;
      case TaskExecutionFragment.TASK_INSTANTIATE_TRANSACTION_2:
        return Transaction.getInstanceFromDb((Long) ids[0]);
      case TaskExecutionFragment.TASK_INSTANTIATE_TEMPLATE:
        Template template = Template.getInstanceFromDb((Long) ids[0]);
        if (template != null) {
          template.prepareForEdit(false);
        }
        return template;
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
        return deleteAccount(anId) ? new Result(true, 0, anId) : Result.FAILURE;
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
          resultMsg += application.getResources().getQuantityString(R.plurals.move_category_success, successCount, successCount);
        }
        if (failureCount > 0) {
          if (!TextUtils.isEmpty(resultMsg)) {
            resultMsg += " ";
          }
          resultMsg += application.getResources().getQuantityString(R.plurals.move_category_failure, failureCount, failureCount);
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
                KEY_INSTANCEID + " = ? AND " + KEY_TEMPLATEID + " = ?",
                new String[]{String.valueOf(ids[i]), String.valueOf(templateId)});
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
          extraInfo2d = (Long[][]) mExtra;
          transactionId = extraInfo2d[i][1];
          Long templateId = extraInfo2d[i][0];
          if (transactionId != null && transactionId > 0L) {
            Transaction.delete(transactionId, false);
          }
          cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
              KEY_INSTANCEID + " = ? AND " + KEY_TEMPLATEID + " = ?",
              new String[]{String.valueOf(ids[i]), String.valueOf(templateId)});
        }
        return null;
      case TaskExecutionFragment.TASK_BACKUP:
        return BackupUtils.doBackup();
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
                Timber.d("Successfully deleted file %s", imageFileUri.toString());
              } else {
                Timber.e("Unable to delete file %s ", imageFileUri.toString());
              }
            } else {
              Timber.d("%s not deleted since it might still be in use", imageFileUri.toString());
            }
            cr.delete(staleImageUri, null, null);
          }
          c.close();
        }
        return null;
      case TaskExecutionFragment.TASK_SAVE_IMAGES:
        File staleFileDir = new File(application.getExternalFilesDir(null), "images.old");
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
                Timber.d("Successfully moved file %s", imageFileUri.toString());
              }
            } else {
              success = true; //we do not move the file but remove its uri from the table
              Timber.d("%s not moved since it might still be in use", imageFileUri.toString());
            }
            if (success) {
              cr.delete(staleImageUri, null, null);
            } else {
              Timber.e("Unable to move file %s", imageFileUri.toString());
            }
          }
          c.close();
        }
        return null;
      case TaskExecutionFragment.TASK_EXPORT_CATEGRIES:
        DocumentFile appDir = AppDirHelper.getAppDir(application);
        if (appDir == null) {
          return new Result(false, R.string.external_storage_unavailable);
        }
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
        String fileName = "categories";
        DocumentFile outputFile = AppDirHelper.timeStampedFile(
            appDir,
            fileName,
            "text/qif", false);
        if (outputFile == null) {
          return new Result(
              false,
              R.string.io_error_unable_to_create_file,
              fileName,
              FileUtils.getPath(application, appDir.getUri()));
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
      case TaskExecutionFragment.TASK_SYNC_UNLINK: {
        String uuid = (String) ids[0];
        if (TextUtils.isEmpty(uuid)) {
          return Result.FAILURE;
        }
        Account account = Account.getInstanceFromDb(Account.findByUuid(uuid));
        AccountManager accountManager = AccountManager.get(application);
        android.accounts.Account syncAccount = GenericAccountService.GetAccount(account.getSyncAccountName());
        accountManager.setUserData(syncAccount, SyncAdapter.KEY_LAST_SYNCED_LOCAL(account.getId()), null);
        accountManager.setUserData(syncAccount, SyncAdapter.KEY_LAST_SYNCED_REMOTE(account.getId()), null);
        account.setSyncAccountName(null);
        account.save();
        return Result.SUCCESS;
      }
      case TaskExecutionFragment.TASK_SYNC_LINK_LOCAL: {
        Account account = Account.getInstanceFromDb(Account.findByUuid((String) ids[0]));
        String syncAccountName = (String) this.mExtra;
        account.setSyncAccountName(syncAccountName);
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putString(KEY_UUID, account.uuid);
        bundle.putBoolean(SyncAdapter.KEY_RESET_REMOTE_ACCOUNT, true);
        ContentResolver.requestSync(GenericAccountService.GetAccount(syncAccountName),
            TransactionProvider.AUTHORITY, bundle);
        account.save();
        return Result.SUCCESS;
      }
      case TaskExecutionFragment.TASK_SYNC_LINK_REMOTE: {
        Account remoteAccount = (Account) this.mExtra;
        if (!deleteAccount(Account.findByUuid(remoteAccount.uuid))) {
          return Result.FAILURE;
        }
        remoteAccount.save();
        return Result.SUCCESS;
      }
      case TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND: {
        AccountManagerFuture<Boolean> accountManagerFuture = AccountManager.get(application).removeAccount(
            GenericAccountService.GetAccount((String) ids[0]), null, null);
        try {
          return accountManagerFuture.getResult() ? Result.SUCCESS : Result.FAILURE;
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
          AcraHelper.report(e);
          return Result.FAILURE;
        }
      }
      case TaskExecutionFragment.TASK_SYNC_LINK_SAVE: {
        //first get remote data for account
        String syncAccountName = ((String) mExtra);
        SyncBackendProvider syncBackendProvider = getSyncBackendProviderFromExtra();
        if (syncBackendProvider == null) {
          return Result.FAILURE;
        }
        List<String> remoteUuidList;
        try {
          Stream<AccountMetaData> remoteAccounStream = syncBackendProvider.getRemoteAccountList();
          remoteUuidList = remoteAccounStream
              .map(AccountMetaData::uuid)
              .collect(Collectors.toList());
        } catch (IOException e) {
          return new Result(false, e.getMessage());
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
          return new Result(false, "Cursor is null");
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
          message = application.getString(R.string.link_account_success, result);
        }
        if (requested > result) {
          message += " " + application.getString(R.string.link_account_failure_1, requested - result)
              + " " + application.getString(R.string.link_account_failure_2)
              + " " + application.getString(R.string.link_account_failure_3);
        }
        return new Result(requested == result, message);
      }
      case TaskExecutionFragment.TASK_SYNC_CHECK: {
        String accountUuid = (String) ids[0];
        SyncBackendProvider syncBackendProvider = getSyncBackendProviderFromExtra();
        if (syncBackendProvider == null) {
          return Result.FAILURE;
        }
        try {
          if (syncBackendProvider.getRemoteAccountList()
              .anyMatch(metadata -> metadata.uuid().equals(accountUuid))) {
            return new Result(false, Utils.concatResStrings(application, " ",
                R.string.link_account_failure_2, R.string.link_account_failure_3)
                + "(" + Utils.concatResStrings(application, ", ", R.string.menu_settings,
                R.string.pref_manage_sync_backends_title) + ")");
          }
          return Result.SUCCESS;
        } catch (IOException e) {
          return new Result(false, e.getMessage());
        }
      }
      case TaskExecutionFragment.TASK_INIT: {
        for (SyncBackendProviderFactory factory : ServiceLoader.load(application)) {
          factory.init();
        }
        if (Utils.hasApiLevel(Build.VERSION_CODES.HONEYCOMB)) {
          //on Gingerbread we just accept that db is initialized with first request
          initDbHoneyComb(cr);
        }
        application.getLicenceHandler().update();
        Account.updateTransferShortcut();
        return null;
      }
      case TaskExecutionFragment.TASK_SETUP_FROM_SYNC_ACCOUNTS: {
        String syncAccountName = (String) mExtra;
        SyncBackendProvider syncBackendProvider = getSyncBackendProviderFromExtra();
        if (syncBackendProvider == null) {
          return Result.FAILURE;
        }
        try {
          List<String> accountUuids = Arrays.asList((String[]) ids);
          int numberOfRestoredAccounts = syncBackendProvider.getRemoteAccountList()
              .filter(accountMetaData -> accountUuids.contains(accountMetaData.uuid()))
              .map(AccountMetaData::toAccount)
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
            ContentResolver.requestSync(GenericAccountService.GetAccount(syncAccountName),
                TransactionProvider.AUTHORITY, bundle);
            return new Result(true);
          }
        } catch (IOException e) {
          return Result.FAILURE;
        }
      }
      case TaskExecutionFragment.TASK_REPAIR_SYNC_BACKEND: {
        Optional<SyncBackendProviderFactory> syncBackendProviderFactoryOptional =
            Stream.of(ServiceLoader.load(application)).filter(factory -> factory.getLabel().equals(ids[0])).findFirst();
        if (syncBackendProviderFactoryOptional.isPresent()) {
          return syncBackendProviderFactoryOptional.get().handleRepairTask(mExtra);
        } else {
          return Result.FAILURE;
        }
      }
      case TaskExecutionFragment.TASK_STORE_SETTING: {
        values = new ContentValues(1);
        values.put(KEY_KEY, (String) ids[0]);
        values.put(KEY_VALUE, (String) mExtra);
        cr.insert(TransactionProvider.SETTINGS_URI, values);
        return null;
      }
    }
    return null;
  }

  @Nullable
  private SyncBackendProvider getSyncBackendProviderFromExtra() {
    String syncAccountName = ((String) mExtra);
    try {
      return SyncBackendProviderFactory.get(MyApplication.getInstance(),
          GenericAccountService.GetAccount(syncAccountName)).getOrThrow();
    } catch (Throwable throwable) {
      AcraHelper.report(new Exception(String.format("Unable to get sync backend provider for %s",
          syncAccountName), throwable));
      return null;
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void initDbHoneyComb(ContentResolver cr) {
    cr.call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_INIT, null, null);
  }

  private boolean deleteAccount(Long anId) {
    try {
      Account.delete(anId);
    } catch (RemoteException | OperationApplicationException e) {
      AcraHelper.reportWithDbSchema(e);
      return false;
    }
    return true;
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