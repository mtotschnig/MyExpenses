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
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * methods both needed by {@link ProtectedFragmentActivity} and {@link ProtectedFragmentActivityNoAppCompat}
 * @author Michael Totschnig
 *
 */
public class ProtectionDelegate {
  Activity ctx;
  public ProtectionDelegate(Activity ctx) {
    this.ctx = ctx;
  }
  protected void handleOnPause(AlertDialog pwDialog) {
    MyApplication app = MyApplication.getInstance();
    if (app.isLocked && pwDialog != null)
      pwDialog.dismiss();
    else {
      app.setmLastPause();
    }
  }
  protected void handleOnDestroy() {
    MyApplication.getInstance().setmLastPause();
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
  public void removeAsyncTaskFragment(FragmentManager m) {
    FragmentTransaction t = m.beginTransaction();
    ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag("PROGRESS"));
    if (f!=null)
      t.remove(f);
    t.remove(m.findFragmentByTag("ASYNC_TASK"));
    t.commitAllowingStateLoss();
  }
}
