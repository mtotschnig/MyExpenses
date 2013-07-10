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
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

public class ProtectedFragmentActivity extends SherlockFragmentActivity
    implements MessageDialogListener, OnSharedPreferenceChangeListener  {
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  private boolean scheduledRestart = false;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
/*    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
    .detectDiskReads()
    .detectDiskWrites()
    .detectNetwork()   // or .detectAll() for all detectable problems
    .penaltyLog()
    .build());
    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
    .detectLeakedSqlLiteObjects()
    .detectLeakedClosableObjects()
    .penaltyLog()
    .penaltyDeath()
    .build());*/

    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getSettings().registerOnSharedPreferenceChangeListener(this);
    protection = new ProtectionDelegate(this);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
    if(scheduledRestart) {
      scheduledRestart = false;
      if (android.os.Build.VERSION.SDK_INT>=11)
        recreate();
      else {
        Intent intent = getIntent();
        startActivity(intent);
        finish();
      }
    } else {
      protection.hanldeOnResume(pwDialog);
    }
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_UI_THEME_KEY)) {
      scheduledRestart = true;
    }
  }

  public void cancelDialog() {
    // TODO Auto-generated method stub
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.common, menu);
    return true;
  }
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
      return dispatchCommand(item.getItemId(),null);
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (CommonCommands.dispatchCommand(this, command))
      return true;
    return false;
  }
}
