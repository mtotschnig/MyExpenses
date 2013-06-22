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
import org.totschnig.myexpenses.fragment.MessageDialogFragment;
import org.totschnig.myexpenses.fragment.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.util.DialogUtils;
import org.totschnig.myexpenses.util.Utils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;

public class Backup extends ProtectedFragmentActivity implements MessageDialogListener,ContribIFace {

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      if (Utils.isExternalStorageAvailable()) {
        if (getIntent().getAction().equals("myexpenses.intent.backup")) {
          File backupDb = MyApplication.getBackupDbFile();
          int message = backupDb.exists() ? R.string.warning_backup_exists : R.string.warning_backup;
          MessageDialogFragment.newInstance(message,R.id.BACKUP_COMMAND,null)
          .show(getSupportFragmentManager(),"BACKUP");
        }
        else {
          //restore
          if (MyApplication.backupExists()) {
            if (MyApplication.getInstance().isContribEnabled) {
              showDialog(R.id.RESTORE_DIALOG);
            }
            else {
              showDialog(R.id.CONTRIB_DIALOG);
            }
          } else {
            Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
            finish();
          }
        }
      }
      else {
        Toast.makeText(getBaseContext(),getString(R.string.external_storage_unavailable), Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case R.id.RESTORE_DIALOG:
      return DialogUtils.createMessageDialog(new ContextThemeWrapper(this, MyApplication.getThemeId()) {
        public void onDialogButtonClicked(View v) {
          dismissDialog(R.id.RESTORE_DIALOG);
          if (v.getId() == R.id.RESTORE_COMMAND) {
            if (MyApplication.backupExists()) {
              MyApplication.backupRestore();
              Feature.RESTORE.recordUsage();
              Intent i = getBaseContext().getPackageManager()
                  .getLaunchIntentForPackage( getBaseContext().getPackageName() );
              i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(i);
            } else {
              Toast.makeText(getBaseContext(),getString(R.string.restore_no_backup_found), Toast.LENGTH_LONG).show();
            }
          }
          finish();
        }
      },R.string.warning_restore,R.id.RESTORE_COMMAND,null)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          finish();
        }
      })
      .create();
    case R.id.CONTRIB_DIALOG:
      return DialogUtils.contribDialog(this,Feature.RESTORE);
    }
    return null;
  }
  @Override
  public void contribFeatureCalled(Feature feature) {
    showDialog(R.id.RESTORE_DIALOG);
  }
  @Override
  public void contribFeatureNotCalled() {
    finish();
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
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
  }
  return true;
  }
  @Override
  public void cancelDialog() {
    finish();
  }
}
