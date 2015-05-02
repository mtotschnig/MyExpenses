package org.totschnig.myexpenses.task;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.io.IOException;
import java.util.ArrayList;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.ExportFormat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;

public class ExportTask extends AsyncTask<Void, String, ArrayList<Uri>> {
  public static final String KEY_FORMAT = "format";
  public static final String KEY_DECIMAL_SEPARATOR = "export_decimal_separator";
  public static final String KEY_NOT_YET_EXPORTED_P = "notYetExportedP";
  public static final String KEY_DELETE_P = "deleteP";
  public static final String KEY_EXPORT_HANDLE_DELETED = "export_handle_deleted";
  public static final String KEY_FILE_NAME = "file_name";
  private final TaskExecutionFragment taskExecutionFragment;
  //we store the label of the account as progress
  private String progress ="";
  private final ArrayList<Uri> result = new ArrayList<>();
  private Account.ExportFormat format;
  private boolean deleteP;
  private boolean notYetExportedP;
  private String dateFormat;
  private char decimalSeparator;
  private long accountId;
  private String currency;
  private String encoding;
  private int handleDelete;
  private WhereFilter filter;
  private String fileName;

  /**
   *
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
    if (deleteP && notYetExportedP)
      throw new IllegalStateException(
          "Deleting exported transactions is only allowed when all transactions are exported");
    try {
      format = ExportFormat.valueOf(extras.getString(KEY_FORMAT));
    } catch (IllegalArgumentException e) {
      format = ExportFormat.QIF;
    }
    accountId = extras.getLong(KEY_ROWID);
    filter = new WhereFilter(extras.getSparseParcelableArray(TransactionList.KEY_FILTER));
    
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
      for (String progress: values) {
        this.taskExecutionFragment.mCallbacks.onProgressUpdate(progress);
      }
    }
  }
  @Override
  protected void onPostExecute(ArrayList<Uri>  result) {
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
    if (accountId > 0L) {
        accountIds = new Long[] {accountId};
    } else {
      String selection = null;
      String[] selectionArgs = null;
      if (currency != null) {
        selection = DatabaseConstants.KEY_CURRENCY + " = ?";
        selectionArgs = new String[]{currency};
      }
      Cursor c = MyApplication.getInstance().getContentResolver().query(TransactionProvider.ACCOUNTS_URI,
          new String[] {KEY_ROWID}, selection, selectionArgs, null);
      accountIds = DbUtils.getLongArrayFromCursor(c, KEY_ROWID);
      c.close();
    }
    Account account;
    DocumentFile destDir;
    DocumentFile appDir = Utils.getAppDir();
    if (appDir == null) {
      publishProgress(MyApplication.getInstance().getString(R.string.external_storage_unavailable));
      return(null);
    }
    if (accountIds.length > 1) {
      destDir = Utils.newDirectory(appDir, fileName);
    } else {
      destDir = appDir;
    }
    ArrayList<Account> successfullyExported = new ArrayList<Account>();
    for (Long id : accountIds) {
      account = Account.getInstanceFromDb(id);
      publishProgress(account.label + " ...");
      try {
        String fileNameForAccount = accountIds.length > 1 ? account.label : fileName;
        Result result = account.exportAll(destDir,fileNameForAccount,format,notYetExportedP,dateFormat,decimalSeparator,encoding,filter);
        String progressMsg;
        if (result.extra != null) {
          progressMsg = MyApplication.getInstance().getString(result.getMessage(),
              FileUtils.getPath(MyApplication.getInstance(), (Uri) result.extra[0]));
        } else {
          progressMsg = MyApplication.getInstance().getString(result.getMessage());
        }
        publishProgress("... " + progressMsg);
        if (result.success) {
          if (MyApplication.PrefKey.PERFORM_SHARE.getBoolean(false)) {
            addResult((Uri) result.extra[0]);
          }
          successfullyExported.add(account);
        }
      } catch (IOException e) {
        publishProgress("... " + MyApplication.getInstance().getString(
            R.string.export_expenses_sdcard_failure,
            appDir.getName(),
            e.getMessage()));
      }
    }
    for (Account a : successfullyExported) {
      if (deleteP) {
        a.reset(filter);
      }
      else {
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
