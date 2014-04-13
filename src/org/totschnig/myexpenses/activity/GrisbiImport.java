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
import org.totschnig.myexpenses.dialog.GrisbiSourcesDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class GrisbiImport extends ProtectedFragmentActivityNoAppCompat {
 
  @Override
  public void onCreate(Bundle savedInstanceState) {
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
      int imported = (int) r.extra[0];
      if (imported>-1) {
        msg = imported == 0 ?
            getString(R.string.import_categories_none) :
            getString(R.string.import_categories_success,imported);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
      }
      imported = (int) r.extra[1];
      if (imported>-1) {
        msg = imported == 0 ?
            getString(R.string.import_parties_none) :
            getString(R.string.import_parties_success,imported);
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
            "ASYNC_TASK")
        .add(ProgressDialogFragment.newInstance(
            0,0,ProgressDialog.STYLE_HORIZONTAL, false),"PROGRESS")
        .commit();
  }
  @Override
  public void onMessageDialogDismissOrCancel() {
    //super.onMessageDialogDismissOrCancel();
    finish();
   }
}
