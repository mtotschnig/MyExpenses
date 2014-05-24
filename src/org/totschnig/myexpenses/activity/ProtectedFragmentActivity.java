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
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.widget.AbstractWidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * @author Michael Totschnig
 *
 */
public class ProtectedFragmentActivity extends ActionBarActivity
    implements MessageDialogListener, OnSharedPreferenceChangeListener,
    TaskExecutionFragment.TaskCallbacks,DbWriteFragment.TaskCallbacks {
  public static final int EDIT_TRANSACTION_REQUEST=1;
  public static final int EDIT_ACCOUNT_REQUEST=2;
  public static final int PREFERENCES_REQUEST=3;
  public static final int CREATE_ACCOUNT_REQUEST=4;
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  private boolean scheduledRestart = false;
  public Enum<?> helpVariant = null;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (MyApplication.debug) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
      .detectDiskReads()
      .detectDiskWrites()
      .detectNetwork()   // or .detectAll() for all detectable problems
      .penaltyLog()
      .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
      .detectLeakedSqlLiteObjects()
      //.detectLeakedClosableObjects()
      .penaltyLog()
      .penaltyDeath()
      .build());
    }

    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getSettings().registerOnSharedPreferenceChangeListener(this);
    setLanguage();
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
  }
  private ProtectionDelegate getProtection() {
    if (protection == null) {
      protection = new ProtectionDelegate(this);
    }
    return protection;
  }
  @Override
  protected void onPause() {
    super.onPause();
    getProtection().handleOnPause(pwDialog);
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    MyApplication.getInstance().getSettings().unregisterOnSharedPreferenceChangeListener(this);
    getProtection().handleOnDestroy();
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
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
      }
    } else {
      pwDialog = getProtection().hanldeOnResume(pwDialog);
    }
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_UI_THEME_KEY) ||
        key.equals(MyApplication.PREFKEY_UI_LANGUAGE) ||
        key.equals(MyApplication.PREFKEY_UI_FONTSIZE)) {
      scheduledRestart = true;
    }
  }

  public void onMessageDialogDismissOrCancel() {
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.common, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      if (dispatchCommand(item.getItemId(),null))
        return true;
      return super.onOptionsItemSelected(item);
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (CommonCommands.dispatchCommand(this, command, tag))
      return true;
    return false;
  }
  @Override
  public void onPreExecute() {
  }
  @Override
  public void onProgressUpdate(Object progress) {
    getProtection().updateProgressDialog(progress);
  }
  @Override
  public void onCancelled() {
    getProtection().removeAsyncTaskFragment(false);
  }
  @Override
  public void onPostExecute(int taskId, Object o) {
    getProtection().removeAsyncTaskFragment(taskId);
  }

  protected void setLanguage() {
    MyApplication.getInstance().setLanguage();
  }
  @Override
  public Model getObject() {
    return null;
  }
  @Override
  public void onPostExecute(Object result) {
    FragmentManager m = getSupportFragmentManager();
    FragmentTransaction t = m.beginTransaction();
    t.remove(m.findFragmentByTag("SAVE_TASK"));
    t.commitAllowingStateLoss();
  }
  
  /**
   * starts the given task, only if no task is currently executed,
   * informs user through toast in that case
   * @param taskId
   * @param objectIds
   * @param extra
   * @param progressMessage if 0 no progress dialog will be shown
   */
  public void startTaskExecution(int taskId, Long[] objectIds, Serializable extra, int progressMessage) {
    getProtection().startTaskExecution(taskId,objectIds,extra,progressMessage);
  }
  
  /**
   * Workaround for broken {@link NavUtils#shouldUpRecreateTask(android.app.Activity, Intent)}
   * @see <a href="http://stackoverflow.com/a/20643984/1199911">http://stackoverflow.com/a/20643984/1199911</a>
   * @param from
   * @return
   */
  protected final boolean shouldUpRecreateTask(Activity from){
    return from.getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET, false);
  }
}
