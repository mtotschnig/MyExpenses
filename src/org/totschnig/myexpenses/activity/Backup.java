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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.util.Utils;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

public class Backup extends ProtectedFragmentActivityNoAppCompat {

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (savedInstanceState == null) {
      if (Utils.isExternalStorageAvailable()) {
        if (getIntent().getAction().equals("myexpenses.intent.backup")) {
          File backupDb = MyApplication.getBackupDbFile();
          int message = backupDb.exists() ? R.string.warning_backup_exists : R.string.warning_backup;
          MessageDialogFragment.newInstance(
              R.string.menu_backup,
              message,
              new MessageDialogFragment.Button(android.R.string.yes, R.id.BACKUP_COMMAND, null),
              null,
              MessageDialogFragment.Button.noButton())
            .show(getSupportFragmentManager(),"BACKUP");
        } else {
          //restore
          if (MyApplication.backupExists()) {
            showRestoreDialog();
          } else {
            Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
          }
        }
      }
      else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }
  private void showRestoreDialog() {
    MessageDialogFragment.newInstance(
        R.string.pref_restore_title,
        R.string.warning_restore,
        new MessageDialogFragment.Button(android.R.string.yes, R.id.RESTORE_COMMAND, null),
        null,
        MessageDialogFragment.Button.noButton())
      .show(getSupportFragmentManager(),"BACKUP");
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command,tag))
      return true;
    switch(command) {
    case R.id.BACKUP_COMMAND:
      if (Utils.isExternalStorageAvailable()) {
        if (MyApplication.getInstance().backup()) {
          Toast.makeText(getBaseContext(),getString(R.string.backup_success), Toast.LENGTH_LONG).show();
        } else {
          Toast.makeText(getBaseContext(),getString(R.string.backup_failure), Toast.LENGTH_LONG).show();
        }
      } else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
      }
      finish();
      break;
    case R.id.RESTORE_COMMAND:
      if (MyApplication.backupExists()) {
        MyApplication.backupRestore();
        setResult(RESULT_FIRST_USER);
        finish();
      } else {
        Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
      }      
  }
  return true;
  }
  @Override
  public void cancelDialog() {
    setResult(RESULT_CANCELED);
    finish();
  }
}
