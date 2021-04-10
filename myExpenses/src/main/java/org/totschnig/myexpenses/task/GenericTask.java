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
import android.net.Uri;
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
import org.totschnig.myexpenses.fragment.AbstractCategoryList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ExportFormat;
import org.totschnig.myexpenses.model.Payee;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncAdapter;
import org.totschnig.myexpenses.sync.SyncBackendProvider;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.BackupUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileCopyUtils;
import org.totschnig.myexpenses.util.io.FileUtils;
import org.totschnig.myexpenses.viewmodel.data.Tag;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;
import static org.totschnig.myexpenses.util.TextUtils.formatQifCategory;

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
      case TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE:
        for (int i = 0; i < ids.length; i++) {
          kotlin.Pair<Transaction, List<Tag>> pair = Transaction.getInstanceFromTemplateWithTags((Long) ids[i]);
          Transaction t = pair.getFirst();
          if (t != null) {
            if (mExtra != null) {
              extraInfo2d = (Long[][]) mExtra;
              final long date = extraInfo2d[i][1] / 1000;
              t.setDate(date);
              t.setValueDate(date);
              t.setOriginPlanInstanceId(extraInfo2d[i][0]);
            }
            t.setStatus(STATUS_NONE);
            if (t.save(true) != null && t.saveTags(pair.getSecond(), cr)) {
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
          CrashHandler.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_UNDELETE_TRANSACTION:
        try {
          for (long id : (Long[]) ids) {
            Transaction.undelete(id);
          }
        } catch (SQLiteConstraintException e) {
          CrashHandler.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_DELETE_ACCOUNT: {
        boolean success = true;
        for (long id : (Long[]) ids) {
          success = success && deleteAccount(id);
        }
        return success ? Result.SUCCESS : Result.FAILURE;
      }
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
      case TaskExecutionFragment.TASK_DELETE_PAYEES:
        try {
          for (long id : (Long[]) ids) {
            Payee.delete(id);
          }
        } catch (SQLiteConstraintException e) {
          CrashHandler.reportWithDbSchema(e);
          return Result.FAILURE;
        }
        return Result.SUCCESS;
      case TaskExecutionFragment.TASK_DELETE_CATEGORY:
        try (Cursor cursor = cr.query(TransactionProvider.CATEGORIES_URI,
            new String[]{KEY_ROWID,
                "(select 1 FROM " + TABLE_TRANSACTIONS + " WHERE " + AbstractCategoryList.CAT_TREE_WHERE_CLAUSE + ") AS " + DatabaseConstants.KEY_MAPPED_TRANSACTIONS,
                "(select 1 FROM " + TABLE_TEMPLATES + " WHERE " + AbstractCategoryList.CAT_TREE_WHERE_CLAUSE + ") AS " + DatabaseConstants.KEY_MAPPED_TEMPLATES
            }, DatabaseConstants.KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(ids.length), Stream.of(((Long[]) ids)).map(String::valueOf).toArray(String[]::new), null)) {
          if (cursor == null) return Result.ofFailure("Cursor is null");
          int deleted = 0, mappedToTransaction = 0, mappedToTemplate = 0;
          if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
              boolean deletable = true;
              if (cursor.getInt(1) > 0) {
                deletable = false;
                mappedToTransaction++;
              }
              if (cursor.getInt(2) > 0) {
                deletable = false;
                mappedToTemplate++;
              }
              if (deletable) {
                Category.delete(cursor.getLong(0));
                deleted++;
              }
              cursor.moveToNext();
            }
            List<String> messages = new ArrayList<>();
            if (deleted > 0) {
              messages.add(context.getResources().getQuantityString(R.plurals.delete_success, deleted, deleted));
            }
            if (mappedToTransaction > 0) {
              messages.add(context.getResources().getQuantityString(
                  R.plurals.not_deletable_mapped_transactions,
                  mappedToTransaction,
                  mappedToTransaction));
            }
            if (mappedToTemplate > 0) {
              messages.add(context.getResources().getQuantityString(
                  R.plurals.not_deletable_mapped_templates,
                  mappedToTemplate,
                  mappedToTemplate));
            }
            return Result.ofSuccess(Stream.of(messages).collect(Collectors.joining(" ")));
          } else {
            return Result.ofFailure("Cursor is empty");
          }
        } catch (SQLiteConstraintException e) {
          CrashHandler.reportWithDbSchema(e);
          return Result.ofFailure(e.getMessage());
        }
      case TaskExecutionFragment.TASK_DELETE_TEMPLATES:
        try {
          for (long id : (Long[]) ids) {
            Template.delete(id, ((Boolean) mExtra));
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
      case TaskExecutionFragment.TASK_CANCEL_PLAN_INSTANCE:
        for (int i = 0; i < ids.length; i++) {
          extraInfo2d = (Long[][]) mExtra;
          transactionId = extraInfo2d[i][1];
          Long templateId = extraInfo2d[i][0];
          if (transactionId != null && transactionId > 0L) {
            Transaction.delete(transactionId, false);
          } else {
            cr.delete(TransactionProvider.PLAN_INSTANCE_SINGLE_URI(templateId, (Long) ids[i]), null, null);
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
          cr.delete(TransactionProvider.PLAN_INSTANCE_SINGLE_URI(templateId, (Long) ids[i]), null, null);
        }
        return null;
      case TaskExecutionFragment.TASK_BACKUP:
        return BackupUtils.doBackup(((String) mExtra), context);
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
      case TaskExecutionFragment.TASK_DELETE_IMAGES: {
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
                CrashHandler.reportWithFormat("Unable to delete file %s ", imageFileUri.toString());
              }
            } else {
              Timber.d("%s not deleted since it might still be in use", imageFileUri.toString());
            }
            cr.delete(staleImageUri, null, null);
          }
          c.close();
        }
        return null;
      }
      case TaskExecutionFragment.TASK_SAVE_IMAGES: {
        File staleFileDir = new File(context.getExternalFilesDir(null), "images.old");
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
                  Timber.e(e);
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
              CrashHandler.reportWithFormat("Unable to move file %s", imageFileUri.toString());
            }
          }
          c.close();
        }
        return null;
      }
      case TaskExecutionFragment.TASK_EXPORT_CATEGORIES:
        DocumentFile appDir = AppDirHelper.getAppDir(context);
        if (appDir == null) {
          return Result.ofFailure(R.string.external_storage_unavailable);
        }
        String mainLabel =
            "CASE WHEN " +
                KEY_PARENTID +
                " THEN " +
                "(SELECT " + KEY_LABEL + " FROM " + TABLE_CATEGORIES + " parent WHERE parent." + KEY_ROWID + " = " + TABLE_CATEGORIES + "." + KEY_PARENTID + ")" +
                " ELSE " + KEY_LABEL +
                " END";
        String subLabel = "CASE WHEN " + KEY_PARENTID +
            " THEN " + KEY_LABEL +
            " END";

        //sort sub categories immediately after their main category
        String sort = "CASE WHEN parent_id then parent_id else _id END";
        String fileName = "categories";
        DocumentFile outputFile = AppDirHelper.timeStampedFile(
            appDir,
            fileName,
            ExportFormat.QIF.getMimeType(), null);
        if (outputFile == null) {
          return Result.ofFailure(
              R.string.io_error_unable_to_create_file,
              fileName,
              FileUtils.getPath(context, appDir.getUri()));
        }
        try {
          OutputStreamWriter out = new OutputStreamWriter(
              cr.openOutputStream(outputFile.getUri()),
              ((String) mExtra));
          c = cr.query(
              Category.CONTENT_URI,
              new String[]{mainLabel, subLabel},
              null, null, sort);
          if (c.getCount() == 0) {
            c.close();
            outputFile.delete();
            return Result.ofFailure(R.string.no_categories);
          }
          out.write("!Type:Cat");
          c.moveToFirst();
          while (c.getPosition() < c.getCount()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nN")
                .append(formatQifCategory(c.getString(0), c.getString(1)))
                .append("\n^");
            out.write(sb.toString());
            c.moveToNext();
          }
          c.close();
          out.close();
          return Result.ofSuccess(R.string.export_sdcard_success, outputFile.getUri(), FileUtils.getPath(context, outputFile.getUri()));
        } catch (IOException e) {
          return Result.ofFailure(R.string.export_sdcard_failure,
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
        final long id = Account.findByUuid(uuid);
        if (id == -1) {
          return Result.FAILURE;
        }
        Account account = Account.getInstanceFromDb(id);
        if (account == null) {
          return Result.FAILURE;
        }
        final String syncAccountName = account.getSyncAccountName();
        if (syncAccountName == null) {
          return Result.FAILURE;
        }
        AccountManager accountManager = AccountManager.get(context);
        android.accounts.Account syncAccount = GenericAccountService.getAccount(syncAccountName);
        accountManager.setUserData(syncAccount, SyncAdapter.KEY_LAST_SYNCED_LOCAL(account.getId()), null);
        accountManager.setUserData(syncAccount, SyncAdapter.KEY_LAST_SYNCED_REMOTE(account.getId()), null);
        account.setSyncAccountName(null);
        account.save();
        return Result.SUCCESS;
      }
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
      case TaskExecutionFragment.TASK_SYNC_LINK_REMOTE: {
        Account remoteAccount = (Account) this.mExtra;
        if (!deleteAccount(Account.findByUuid(remoteAccount.getUuid()))) {
          return Result.FAILURE;
        }
        remoteAccount.save();
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
          Stream<AccountMetaData> remoteAccounStream = syncBackendProvider.get().getRemoteAccountList()
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
      case TaskExecutionFragment.TASK_SYNC_CHECK: {
        String accountUuid = (String) ids[0];
        Exceptional<SyncBackendProvider> syncBackendProvider = getSyncBackendProviderFromExtra();
        if (!syncBackendProvider.isPresent()) {
          return Result.ofFailure(syncBackendProvider.getException().getMessage());
        }
        try {
          if (syncBackendProvider.get().getRemoteAccountList()
              .filter(Exceptional::isPresent)
              .map(Exceptional::get)
              .anyMatch(metadata -> metadata.uuid().equals(accountUuid))) {
            return Result.ofFailure(concatResStrings(context, " ",
                R.string.link_account_failure_2, R.string.link_account_failure_3)
                + "(" + concatResStrings(context, ", ", R.string.menu_settings,
                R.string.pref_manage_sync_backends_title) + ")");
          }
          return Result.SUCCESS;
        } catch (IOException e) {
          return Result.ofFailure(e.getMessage());
        } finally {
          syncBackendProvider.get().tearDown();
        }
      }
      case TaskExecutionFragment.TASK_SETUP_FROM_SYNC_ACCOUNTS: {
        String syncAccountName = (String) mExtra;
        Exceptional<SyncBackendProvider> syncBackendProvider = getSyncBackendProviderFromExtra();
        if (!syncBackendProvider.isPresent()) {
          return Result.ofFailure(syncBackendProvider.getException().getMessage());
        }
        try {
          List<String> accountUuids = Arrays.asList((String[]) ids);
          int numberOfRestoredAccounts = syncBackendProvider.get().getRemoteAccountList()
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
      case TaskExecutionFragment.TASK_STORE_SETTING: {
        DbUtils.storeSetting(cr, (String) ids[0], (String) mExtra);
        return null;
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
    String syncAccountName = ((String) mExtra);
    try {
      final android.accounts.Account account = GenericAccountService.getAccount(syncAccountName);
      final Context context = MyApplication.getInstance();
      return Exceptional.of(() -> SyncBackendProviderFactory.get(context, account, false).getOrThrow());
    } catch (Throwable throwable) {
      CrashHandler.report(new Exception(String.format("Unable to get sync backend provider for %s",
          syncAccountName), throwable));
      return Exceptional.of(throwable);
    }
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