package org.totschnig.myexpenses.task;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.export.Exporter;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ExportFormat;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.documentfile.provider.DocumentFile;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class ExportTask extends AsyncTask<Void, String, ArrayList<Uri>> {
  public static final String KEY_DECIMAL_SEPARATOR = "export_decimal_separator";
  public static final String KEY_NOT_YET_EXPORTED_P = "notYetExportedP";
  public static final String KEY_DELETE_P = "deleteP";
  public static final String KEY_EXPORT_HANDLE_DELETED = "export_handle_deleted";
  public static final String KEY_FILE_NAME = "file_name";
  public static final String KEY_DELIMITER = "export_delimiter";
  public static final String KEY_MERGE_P = "export_merge_accounts";
  private final TaskExecutionFragment taskExecutionFragment;
  //we store the label of the account as progress
  private String progress = "";
  private final ArrayList<Uri> result = new ArrayList<>();
  private ExportFormat format;
  private boolean deleteP;
  private boolean notYetExportedP;
  private boolean mergeP;
  private String dateFormat;
  private char decimalSeparator;
  private long accountId;
  private String currency;
  private String encoding;
  private int handleDelete;
  private WhereFilter filter;
  private String fileName;
  private char delimiter;

  /**
   * @param taskExecutionFragment
   * @param extras
   */
  public ExportTask(TaskExecutionFragment taskExecutionFragment, Bundle extras) {
    this.taskExecutionFragment = taskExecutionFragment;
    deleteP = extras.getBoolean(KEY_DELETE_P);
    notYetExportedP = extras.getBoolean(KEY_NOT_YET_EXPORTED_P);
    dateFormat = extras.getString(TaskExecutionFragment.KEY_DATE_FORMAT);
    decimalSeparator = extras.getChar(KEY_DECIMAL_SEPARATOR);
    encoding = extras.getString(TaskExecutionFragment.KEY_ENCODING);
    currency = extras.getString(KEY_CURRENCY);
    fileName = extras.getString(KEY_FILE_NAME);
    handleDelete = extras.getInt(KEY_EXPORT_HANDLE_DELETED);
    delimiter = extras.getChar(KEY_DELIMITER);
    mergeP = extras.getBoolean(KEY_MERGE_P);
    if (deleteP && notYetExportedP)
      throw new IllegalStateException(
          "Deleting exported transactions is only allowed when all transactions are exported");
    try {
      format = ExportFormat.valueOf(extras.getString(TaskExecutionFragment.KEY_FORMAT));
    } catch (IllegalArgumentException e) {
      format = ExportFormat.QIF;
    }
    accountId = extras.getLong(KEY_ROWID);
    filter = new WhereFilter(extras.getParcelableArrayList(TransactionList.KEY_FILTER));

  }

  String getProgress() {
    return progress;
  }

  void appendToProgress(String progress) {
    this.progress += "\n" + progress;
  }

  /* (non-Javadoc)
   * updates the progress dialog
   * @see android.os.AsyncTask#onProgressUpdate(Progress[])
   */
  @Override
  protected void onProgressUpdate(String... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      for (String progress : values) {
        this.taskExecutionFragment.mCallbacks.onProgressUpdate(progress);
      }
    }
  }

  @Override
  protected void onPostExecute(ArrayList<Uri> result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_EXPORT, result);
    }
  }

  /* (non-Javadoc)
   * this is where the bulk of the work is done via calls to {@link #importCatsMain()}
   * and {@link #importCatsSub()}
   * sets up {@link #categories} and {@link #sub_categories}
   * @see android.os.AsyncTask#doInBackground(Params[])
   */
  @Override
  protected ArrayList<Uri> doInBackground(Void... ignored) {
    Long[] accountIds;
    MyApplication application = MyApplication.getInstance();
    if (accountId > 0L) {
      accountIds = new Long[]{accountId};
    } else {
      String selection = null;
      String[] selectionArgs = null;
      if (currency != null) {
        selection = DatabaseConstants.KEY_CURRENCY + " = ?";
        selectionArgs = new String[]{currency};
      }
      Cursor c = application.getContentResolver().query(TransactionProvider.ACCOUNTS_URI,
          new String[]{KEY_ROWID}, selection, selectionArgs, null);
      accountIds = DbUtils.getLongArrayFromCursor(c, KEY_ROWID);
      c.close();
    }
    Account account;
    DocumentFile destDir;
    DocumentFile appDir = AppDirHelper.getAppDir(application);
    if (appDir == null) {
      publishProgress(application.getString(R.string.external_storage_unavailable));
      return (null);
    }
    boolean oneFile = accountIds.length == 1 || mergeP;
    if (oneFile) {
      destDir = appDir;
    } else {
      destDir = AppDirHelper.newDirectory(appDir, fileName);
    }
    ArrayList<Account> successfullyExported = new ArrayList<>();
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyMMdd-HHmmss", Locale.US);
    for (int i = 0; i < accountIds.length; i++) {
      account = Account.getInstanceFromDb(accountIds[i]);
      if (account == null) continue;
      publishProgress(account.getLabel() + " ...");
      try {
        String fileNameForAccount = oneFile ? fileName :
            String.format(("%s-%s"), Utils.escapeForFileName(account.getLabel()),
                simpleDateFormat.format(new Date()));
        Result<Uri> result = new Exporter(account, filter, destDir, fileNameForAccount, format,
            notYetExportedP, dateFormat, decimalSeparator, encoding, delimiter, mergeP && i > 0, mergeP).export();
        publishProgress("... " + result.print(application));
        if (result.isSuccess()) {
          if (PrefKey.PERFORM_SHARE.getBoolean(false)) {
            addResult(result.getExtra());
          }
          successfullyExported.add(account);
        }
      } catch (IOException e) {
        publishProgress("... " + application.getString(
            R.string.export_sdcard_failure,
            appDir.getName(),
            e.getMessage()));
      }
    }
    for (Account a : successfullyExported) {
      if (deleteP) {
        if (a.isSealed()) {
          CrashHandler.report("Trying to reset account that is sealed");
        } else {
          a.reset(filter, handleDelete, fileName);
        }
      } else {
        a.markAsExported(filter);
      }
    }
    return getResult();
  }

  public ArrayList<Uri> getResult() {
    return result;
  }

  public void addResult(Uri fileUri) {
    result.add(fileUri);
  }
}
