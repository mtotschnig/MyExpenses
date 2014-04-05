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

public class QifImport extends ProtectedFragmentActivityNoAppCompat {

  public static final int IMPORT_FILENAME_REQUESTCODE = 1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      QifImportDialogFragment.newInstance().show(getSupportFragmentManager(), "QIF_IMPORT_SOURCE");
    }
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
  @Override

  public void onMessageDialogDismissOrCancel() {
   finish();
  }
}
