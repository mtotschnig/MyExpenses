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
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public abstract class ProtectedFragmentActivity extends SherlockFragmentActivity
    implements MessageDialogListener {
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    protection = new ProtectionDelegate(this);
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
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
    protection.hanldeOnResume(pwDialog);
  }
  public void showContribDialog(final Feature feature, Serializable tag) {
    ContribDialogFragment.newInstance(feature, tag).show(getSupportFragmentManager(),"CONTRIB");
  }

  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case R.id.CONTRIB_PLAY_COMMAND:
      Utils.viewContribApp(this);
      return true;
    case R.id.WEB_COMMAND:
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://" + MyApplication.HOST));
      startActivity(i);
      return true;
    }
    return false;
  }
  public void cancelDialog() {
    // TODO Auto-generated method stub
  }
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
    case android.R.id.home:
      setResult(RESULT_CANCELED);
      finish();
      return true;
    case R.id.SETTINGS_COMMAND:
      startActivityForResult(new Intent(this, MyPreferenceActivity.class),0);
      return true;
    case R.id.HELP_COMMAND:
      showHelpDialog();
     return true;
   default:
     return false;
    }
  }
  public void showHelpDialog() {
    HelpDialogFragment.newInstance(getVersionInfo())
      .show(getSupportFragmentManager(),"HELP");
  }
  /**
   * retrieve information about the current version
   * @return concatenation of versionName, versionCode and buildTime
   * buildTime is automatically stored in property file during build process
   */
  public String getVersionInfo() {
    String version = "";
    String versionname = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = " (revision " + pi.versionCode + ") ";
      versionname = pi.versionName;
      //versiontime = ", " + R.string.installed + " " + sdf.format(new Date(pi.lastUpdateTime));
    } catch (Exception e) {
      Log.e("MyExpenses", "Package info not found", e);
    }
    return versionname + version  + MyApplication.BUILD_DATE;
  }
  /**
   * @return version name
   */
  public String getVersionName() {
    String version = "";
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionName;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return version;
  }
  /**
   * @return version number (versionCode)
   */
  public int getVersionNumber() {
    int version = -1;
    try {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = pi.versionCode;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return version;
  }

  public void onDialogButtonClicked(View v) {
    dispatchCommand(v.getId(),v.getTag());
  }
}
