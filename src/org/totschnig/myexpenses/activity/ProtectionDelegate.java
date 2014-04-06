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

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

/**
 * methods both needed by {@link ProtectedFragmentActivity} and {@link ProtectedFragmentActivityNoAppCompat}
 * @author Michael Totschnig
 *
 */
public class ProtectionDelegate {
  Activity ctx;
  private String progress = "";
  public ProtectionDelegate(Activity ctx) {
    this.ctx = ctx;
  }
  protected void handleOnPause(AlertDialog pwDialog) {
    MyApplication app = MyApplication.getInstance();
    if (app.isLocked && pwDialog != null)
      pwDialog.dismiss();
    else {
      app.setLastPause();
    }
  }
  protected void handleOnDestroy() {
    MyApplication.getInstance().setLastPause();
  }
  protected AlertDialog hanldeOnResume(AlertDialog pwDialog) {
    MyApplication app = MyApplication.getInstance();
    if (app.shouldLock()) {
      if (pwDialog == null)
        pwDialog = DialogUtils.passwordDialog(ctx);
      DialogUtils.showPasswordDialog(ctx,pwDialog);
    }
    return pwDialog;
  }

  public void removeAsyncTaskFragment(boolean keepProgress) {
    FragmentManager m = ((FragmentActivity) ctx).getSupportFragmentManager();
    FragmentTransaction t = m.beginTransaction();
    ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag("PROGRESS"));
    if (f!=null) {
      if (keepProgress) {
        f.onTaskCompleted();
      } else {
        t.remove(f);
      }
    }
    t.remove(m.findFragmentByTag("ASYNC_TASK"));
    t.commitAllowingStateLoss();
  }
  public void removeAsyncTaskFragment(int taskId) {
    removeAsyncTaskFragment(taskId == TaskExecutionFragment.TASK_QIF_IMPORT ||
        taskId == TaskExecutionFragment.TASK_EXPORT);
  }
  public void updateProgressDialog(Object progress) {
    FragmentManager m = ((FragmentActivity) ctx).getSupportFragmentManager();
    ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag("PROGRESS"));
    if (f!=null) {
      if (progress instanceof Integer) {
        f.setProgress((Integer) progress);
      } else if (progress instanceof String) {
        appendToProgress((String) progress);
        f.setMessage(getProgress());
      }
    }
  }
  void appendToProgress(String progress) {
    this.progress += "\n" + progress;
  }
  String getProgress() {
    return progress;
  }
  void clearProgress() {
    progress ="";
  }
  public void startTaskExecution(int taskId, Long[] objectIds,
      Serializable extra, int progressMessage) {
    FragmentManager m = ((FragmentActivity) ctx).getSupportFragmentManager();
    if (m.findFragmentByTag("ASYNC_TASK") != null) {
      Toast.makeText(ctx.getBaseContext(),
          "Previous task still executing, please try again later",
          Toast.LENGTH_LONG)
          .show();
    } else {
      FragmentTransaction ft = m.beginTransaction()
        .add(TaskExecutionFragment.newInstance(
            taskId,
            objectIds, extra),
          "ASYNC_TASK");
      if (progressMessage != 0) {
        ft.add(ProgressDialogFragment.newInstance(progressMessage),"PROGRESS");
      }
      ft.commit();
    }
  }
}
