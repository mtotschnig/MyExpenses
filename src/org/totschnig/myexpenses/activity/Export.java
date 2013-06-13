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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.DataObjectNotFoundException;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Export extends ProtectedActivity {
  ProgressDialog mProgressDialog;
  private MyAsyncTask task=null;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle extras = getIntent().getExtras();
    Account account;
    try {
      account = Account.getInstanceFromDb(extras.getLong(DatabaseConstants.KEY_ROWID));
    } catch (DataObjectNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    mProgressDialog.setTitle(R.string.pref_category_title_export);
    mProgressDialog.setIndeterminate(true);
    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    mProgressDialog.setCancelable(false);
    mProgressDialog.show();
    
    task=(MyAsyncTask)getLastNonConfigurationInstance();
    
    if (task!=null) {
      task.attach(this);
      updateProgress(task.getProgress());
      
      if (task.getStatus() == AsyncTask.Status.FINISHED) {
        markAsDone();
      }
    } else if (savedInstanceState == null) {
      task = new MyAsyncTask(this);
      task.execute(account);
    }
  }
  @Override
  public void onStop() {
    super.onStop();
    mProgressDialog.dismiss();
  }
  void updateProgress(String string) {
    mProgressDialog.setMessage(string);
  }

  void markAsDone() {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    mProgressDialog.dismiss();
    ArrayList<File> files = task.getResult();
    if (files != null && files.size() >0)
      Utils.share(this,files, MyApplication.getInstance().getSettings().getString(MyApplication.PREFKEY_SHARE_TARGET,"").trim());
    task = null;
    finish();
  }
  
  @Override
  public Object onRetainNonConfigurationInstance() {
    if (task != null)
      task.detach();
    return(task);
  }

  /**
   * This AsyncTaks has an Account as input upon execution
   * reports Strings for showing a progress update
   * and gives no result
   * @author Michael Totschnig
   *
   */
  static class MyAsyncTask extends AsyncTask<Account, String, Void> {
    private Export activity;
    private int max;
    //we store the label of the account as progress
    private String progress ="";
    ArrayList<File> result;

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
    protected Void doInBackground(Account... account) {
      ArrayList<File> files = new ArrayList<File>();
      publishProgress(account[0].label + " ...");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      try {
        File output = account[0].exportAll();
        SharedPreferences settings = MyApplication.getInstance().getSettings();
        if (output != null) {
          publishProgress("... " + String.format(activity.getString(R.string.export_expenses_sdcard_success), output.getAbsolutePath()));
          try {
            Thread.sleep(500);
          } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
          if (settings.getBoolean(MyApplication.PREFKEY_PERFORM_SHARE,false)) {
            files.add(output);
          }
          account[0].reset();
          setResult(files);
        }
      } catch (IOException e) {
        Log.e("MyExpenses",e.getMessage());
        Toast.makeText(activity,activity.getString(R.string.export_expenses_sdcard_failure), Toast.LENGTH_LONG).show();
      }
      return(null);
    }
    public ArrayList<File> getResult() {
      return result;
    }
    public void setResult(ArrayList<File> result) {
      this.result = result;
    }
  }
}
