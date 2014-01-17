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

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;


public class ProtectedPreferenceActivity extends PreferenceActivity {
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  
  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    protection = new ProtectionDelegate(this);
    MyApplication.getInstance().setLanguage();
    if (Build.VERSION.SDK_INT > 10) {
      ActionBar actionBar = getActionBar();
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
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
    pwDialog = protection.hanldeOnResume(pwDialog);
  }
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
    case android.R.id.home:
      setResult(RESULT_CANCELED);
      finish();
      return true;
   default:
     return false;
    }
  }
}
