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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.QifImportDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class QifImport extends ProtectedFragmentActivityNoAppCompat implements
    TaskExecutionFragment.TaskCallbacks {

  public static final int IMPORT_FILENAME_REQUESTCODE = 1;
  
  private String progress = "";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      QifImportDialogFragment.newInstance().show(getSupportFragmentManager(), "QIF_IMPORT_SOURCE");
    }
  }
  @Override
  public void onProgressUpdate(Object progress) {
    FragmentManager fm = getSupportFragmentManager();
    ProgressDialogFragment f = (ProgressDialogFragment) fm.findFragmentByTag("PROGRESS");
    if (f != null) {
      appendToProgress((String) progress);
      f.setMessage(getProgress());
    }
  }

  @Override
  public void onPostExecute(int taskId,Object result) {
    FragmentManager fm = getSupportFragmentManager();
    FragmentTransaction t = fm.beginTransaction();
    t.remove(fm.findFragmentByTag("ASYNC_TASK"));
    t.commitAllowingStateLoss();
    ProgressDialogFragment f = (ProgressDialogFragment) fm.findFragmentByTag("PROGRESS");
    if (fm != null) {
      f.onTaskCompleted();
    }
  }

  @Override
  public void onPreExecute() {
  }

  @Override
  public void onCancelled() {
  }

  public void onSourceSelected(String filePath, QifDateFormat qifDateFormat,
      long accountId) {
    getSupportFragmentManager()
      .beginTransaction()
      .add(TaskExecutionFragment.newInstanceQifImport(filePath, qifDateFormat, accountId),
          "ASYNC_TASK")
      .add(ProgressDialogFragment.newInstance(
          R.string.pref_import_qif_title,0,ProgressDialog.STYLE_SPINNER,true),"PROGRESS")
      .commit();
  }

  void appendToProgress(String progress) {
    this.progress += "\n" + progress;
  }
  String getProgress() {
    return progress;
  }
}
