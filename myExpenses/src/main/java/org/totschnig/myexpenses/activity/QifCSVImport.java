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
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.QifCsvImportDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.ExportFormat;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;

public class QifCSVImport extends ProtectedFragmentActivity {

  private ExportFormat format = ExportFormat.CSV;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdTranslucent());
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
     /* try {
        format = Account.ExportFormat.valueOf(getIntent().getStringExtra(TaskExecutionFragment.KEY_FORMAT));
      } catch (IllegalArgumentException e) {
        format = Account.ExportFormat.QIF;
      }*/
      QifCsvImportDialogFragment.newInstance(format).show(getSupportFragmentManager(), "QIF_IMPORT_SOURCE");
    }
  }

  public void onSourceSelected(
      Uri mUri,
      QifDateFormat qifDateFormat,
      long accountId,
      String currency,
      boolean withTransactions,
      boolean withCategories,
      boolean withParties, String encoding) {
    TaskExecutionFragment taskExecutionFragment = /*format.equals(Account.ExportFormat.QIF) ?*/
        TaskExecutionFragment.newInstanceQifImport(
            mUri, qifDateFormat, accountId, currency, withTransactions,
            withCategories, withParties, encoding)/* :
        TaskExecutionFragment.newInstanceCSVParse(
            mUri, qifDateFormat, accountId, currency, encoding)*/;
    getSupportFragmentManager()
        .beginTransaction()
        .add(taskExecutionFragment,
            ProtectionDelegate.ASYNC_TAG)
        .add(ProgressDialogFragment.newInstance(
                getString(R.string.pref_import_title, format.name()),
                null, ProgressDialog.STYLE_SPINNER, true),
            ProtectionDelegate.PROGRESS_TAG)
        .commit();
  }

  @Override

  public void onMessageDialogDismissOrCancel() {
    super.onMessageDialogDismissOrCancel();
    finish();
  }
}
