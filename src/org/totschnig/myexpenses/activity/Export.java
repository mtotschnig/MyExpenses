/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.activity;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.ExportFormat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.ScrollableProgressDialog;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * if called with KEY_ROW_ID in extras export one account, all otherwise
 *
 */
public class Export extends ProtectedFragmentActivityNoAppCompat {
  ProgressDialog mProgressDialog;
  private MyAsyncTask task=null;
  /**
   * we set this to true once the task is finished, but we keep it alive until the user dismisses the dialog
   * to have access to its message
   */
  private boolean mDone = false;
  private Account.ExportFormat format;
  private boolean deleteP;
  private boolean notYetExportedP;
  private String dateFormat;
  private char decimalSeparator;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle extras = getIntent().getExtras();
    deleteP = extras.getBoolean("deleteP");
    notYetExportedP = extras.getBoolean("notYetExportedP");
    dateFormat = extras.getString("dateFormat");
    decimalSeparator = extras.getChar("decimalSeparator");
    if (deleteP && notYetExportedP)
      throw new IllegalStateException(
          "Deleting exported transactions is only allowed when all transactions are exported");
    try {
      format = ExportFormat.valueOf(extras.getString("format"));
    } catch (IllegalArgumentException e) {
      format = ExportFormat.QIF;
    }
    Long[] accountIds;
    Long accountId = extras.getLong(KEY_ROWID);
    if (accountId > 0L) {
        accountIds = new Long[] {accountId};
    } else {
      String selection = null;
      String[] selectionArgs = null;
      String currency = extras.getString(KEY_CURRENCY);
      if (currency != null) {
        selection = DatabaseConstants.KEY_CURRENCY + " = ?";
        selectionArgs = new String[]{currency};
      }
      Cursor c = getContentResolver().query(TransactionProvider.ACCOUNTS_URI,
          new String[] {KEY_ROWID}, selection, selectionArgs, null);
      accountIds = DbUtils.getLongArrayFromCursor(c, KEY_ROWID);
    }
    //Applying the dark/light theme only works starting from 11, below, the dialog uses a dark theme
    Context wrappedCtx = DialogUtils.wrapContext2(this);
    mProgressDialog = new ScrollableProgressDialog(wrappedCtx);
    mProgressDialog.setTitle(R.string.pref_category_title_export);
    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    mProgressDialog.setCancelable(false);
    mProgressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(android.R.string.ok),
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog,int whichButton) {
            mProgressDialog.dismiss();
            task = null;
            setResult(RESULT_OK);
            finish();
          }
      });
    mProgressDialog.show();
    mProgressDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
    
    task=(MyAsyncTask)getLastCustomNonConfigurationInstance();
    
    if (task!=null) {
      task.attach(this);
      updateProgress(task.getProgress());
      
      if (task.getStatus() == AsyncTask.Status.FINISHED) {
        mDone = savedInstanceState.getBoolean("Done",false);
        if (mDone) {
          mProgressDialog.setIndeterminateDrawable(null);
          mProgressDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
        } else
          markAsDone();
      }
    } else if (savedInstanceState == null) {
      task = new MyAsyncTask(this);
      task.execute(accountIds);
    }
  }
  void updateProgress(String string) {
    mProgressDialog.setMessage(string);
  }

  void markAsDone() {
    //this hides the spinner, setting a different drawable did not work
    mProgressDialog.setIndeterminateDrawable(null);
    ArrayList<File> files = task.getResult();
    if (files != null && files.size() >0)
      Utils.share(this,files,
          MyApplication.getInstance().getSettings().getString(MyApplication.PREFKEY_SHARE_TARGET,"").trim(),
          "text/" + format.name().toLowerCase(Locale.US));
    mProgressDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
    mDone = true;
  }
  
  @Override
  public Object onRetainCustomNonConfigurationInstance() {
    if (task != null)
      task.detach();
    return(task);
  }
  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putBoolean("Done", mDone);
  }

  /**
   * This AsyncTaks has an Account as input upon execution
   * reports Strings for showing a progress update
   * and gives no result
   * @author Michael Totschnig
   *
   */
  static class MyAsyncTask extends AsyncTask<Long, String, Void> {
    private Export activity;
    //we store the label of the account as progress
    private String progress ="";
    private final ArrayList<File> result = new ArrayList<File>();

    /**
     * @param context
     * @param source Source for the import
     */
    public MyAsyncTask(Export activity) {
      attach(activity);
    }
    void attach(Export activity2) {
      this.activity=activity2;
    }
    void detach() {
      activity=null;
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
    protected void onProgressUpdate(String... values) {
      if (activity==null) {
        Log.w("MyAsyncTask", "onProgressUpdate() skipped -- no activity");
      }
      else {
        for (String progress: values)
          appendToProgress(progress);
        activity.updateProgress(getProgress());
      }
    }
    @Override
    protected void onPostExecute(Void unused) {
      if (activity==null) {
        Log.w("MyAsyncTask", "onPostExecute() skipped -- no activity");
      }
      else {
        activity.markAsDone();
      }
    }

    /* (non-Javadoc)
     * this is where the bulk of the work is done via calls to {@link #importCatsMain()}
     * and {@link #importCatsSub()}
     * sets up {@link #categories} and {@link #sub_categories}
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Void doInBackground(Long... accountIds) {
      Account account;
      File destDir;
      File appDir = Utils.requireAppDir();
      if (appDir == null) {
        publishProgress(activity.getString(R.string.external_storage_unavailable));
        return(null);
      }
      if (accountIds.length > 1) {
        String now = new SimpleDateFormat("yyyMMdd-HHmmss",Locale.US).format(new Date());
        destDir = new File(appDir,"export-" + now);
        if (destDir.exists()) {
          publishProgress(String.format(activity.getString(R.string.export_expenses_outputfile_exists), destDir.getAbsolutePath()));
          return(null);
        }
        destDir.mkdir();
      } else
        destDir = appDir;
      ArrayList<Account> successfullyExported = new ArrayList<Account>();
      for (Long id : accountIds) {
        account = Account.getInstanceFromDb(id);
        publishProgress(account.label + " ...");
        try {
          Result result = account.exportAll(destDir,activity.format,activity.notYetExportedP,activity.dateFormat,activity.decimalSeparator);
          File output = null;
          String progressMsg;
          if (result.extra != null) {
            output = (File) result.extra[0];
            progressMsg = activity.getString(result.getMessage(), output.getAbsolutePath());
          } else {
            progressMsg = activity.getString(result.getMessage());
          }
          publishProgress("... " + progressMsg);
          if (result.success) {
            SharedPreferences settings = MyApplication.getInstance().getSettings();
            if (settings.getBoolean(MyApplication.PREFKEY_PERFORM_SHARE,false)) {
              addResult(output);
            }
            successfullyExported.add(account);
          }
        } catch (IOException e) {
          Log.e("MyExpenses",e.getMessage());
          publishProgress("... " + activity.getString(R.string.export_expenses_sdcard_failure));
        }
      }
      for (Account a : successfullyExported) {
        if (activity.deleteP)
          a.reset();
        else
          a.markAsExported();
      }
      return(null);
    }
    public ArrayList<File> getResult() {
      return result;
    }
    public void addResult(File file) {
      result.add(file);
    }
  }
}
