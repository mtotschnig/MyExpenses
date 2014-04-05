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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * This is the parent class for activities that use
 * the translucent Translucent.NoTitleBar theme that is not available
 * through AppCompat and hence need to inherit from the Base Fragment Activity
 *
 */
public class ProtectedFragmentActivityNoAppCompat extends FragmentActivity implements
    MessageDialogListener,TaskExecutionFragment.TaskCallbacks  {
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    protection = new ProtectionDelegate(this);
    setLanguage();
  }
  @Override
  protected void onPause() {
    super.onPause();
    protection.handleOnPause(pwDialog);
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    protection.handleOnDestroy();
  }
  @Override
  protected void onResume() {
    super.onResume();
    pwDialog = protection.hanldeOnResume(pwDialog);
  }
  public void cancelDialog() {
    // TODO Auto-generated method stub
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (CommonCommands.dispatchCommand(this, command))
      return true;
    return false;
  }
  protected void setLanguage() {
    MyApplication.getInstance().setLanguage();
  }
  @Override
  public void onCancelled() {
    protection.removeAsyncTaskFragment(getSupportFragmentManager());
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    protection.removeAsyncTaskFragment(getSupportFragmentManager());
  }
  @Override
  public void onPreExecute() {
  }
  @Override
  public void onProgressUpdate(Object progress) {
  }
}
