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

import org.totschnig.myexpenses.dialog.GrisbiSourcesDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

public class GrisbiImport extends ProtectedFragmentActivityNoAppCompat implements
    TaskExecutionFragment.TaskCallbacks {
 
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      GrisbiSourcesDialogFragment.newInstance().show(getSupportFragmentManager(), "GRISBI_SOURCES");
    }
  }
  @Override
  public void onProgressUpdate(Object progress) {
    FragmentManager fm = getSupportFragmentManager();
    ProgressDialogFragment f = (ProgressDialogFragment) fm.findFragmentByTag("PROGRESS");
    if (f != null) {
      f.setProgress((Integer) progress);
    }
  }
  public void setProgressMax(int max) {
    FragmentManager fm = getSupportFragmentManager();
    ProgressDialogFragment f = (ProgressDialogFragment) fm.findFragmentByTag("PROGRESS");
    if (f != null) {
      f.setMax(max);
    }
  }
  public void setProgressTitle(String title) {
    FragmentManager fm = getSupportFragmentManager();
    ProgressDialogFragment f = (ProgressDialogFragment) fm.findFragmentByTag("PROGRESS");
    if (f != null) {
      f.setTitle(title);
    }
  }
  @Override
  public void onPostExecute(int taskId,Object result) {
    super.onPostExecute(taskId,result);
    String msg;
    msg = ((Result) result).print(this);
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    finish();
  }
  @Override
  public void onPreExecute() {
  }

  @Override
  public void onCancelled() {
  }

  public void onSourceSelected(boolean external, boolean withParties) {
    getSupportFragmentManager()
      .beginTransaction()
        .add(TaskExecutionFragment.newInstanceGrisbiImport(external, withParties),
            "ASYNC_TASK")
        .add(ProgressDialogFragment.newInstance(0,ProgressDialog.STYLE_HORIZONTAL),"PROGRESS")
        .commit();
  }
}
