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
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.GrisbiSourcesDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class GrisbiImport extends ProtectedFragmentActivity {
 
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdTranslucent());
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      GrisbiSourcesDialogFragment.newInstance().show(getSupportFragmentManager(), "GRISBI_SOURCES");
    }
  }
  @Override
  public void onPostExecute(int taskId,Object result) {
    super.onPostExecute(taskId,result);
    Result r = (Result) result;
    String msg;
    if (r.success) {
      Integer imported = (Integer) r.extra[0];
      if (imported>-1) {
        msg = imported == 0 ?
            getString(R.string.import_categories_none) :
            getString(R.string.import_categories_success, String.valueOf(imported));
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
      }
      imported = (Integer) r.extra[1];
      if (imported>-1) {
        msg = imported == 0 ?
            getString(R.string.import_parties_none) :
            getString(R.string.import_parties_success, String.valueOf(imported));
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
      }
    } else {
      msg = r.print(this);
      Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    finish();
  }

  public void onSourceSelected(Uri mUri, boolean withCategories, boolean withParties) {
    getSupportFragmentManager()
      .beginTransaction()
        .add(TaskExecutionFragment.newInstanceGrisbiImport(true, mUri, withCategories, withParties),
            ProtectionDelegate.ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(
            0,0,ProgressDialog.STYLE_HORIZONTAL, false),ProtectionDelegate.PROGRESS_TAG)
        .commit();
  }
  @Override
  public void onMessageDialogDismissOrCancel() {
    //super.onMessageDialogDismissOrCancel();
    finish();
   }
}
