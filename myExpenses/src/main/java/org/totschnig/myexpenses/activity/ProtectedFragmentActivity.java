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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.AbstractWidget;

import java.io.Serializable;

/**
 * @author Michael Totschnig
 *
 */
public class ProtectedFragmentActivity extends AppCompatActivity
    implements MessageDialogListener, OnSharedPreferenceChangeListener,
    TaskExecutionFragment.TaskCallbacks,DbWriteFragment.TaskCallbacks {
  public static final int CALCULATOR_REQUEST = 0;
  public static final int EDIT_TRANSACTION_REQUEST=1;
  public static final int EDIT_ACCOUNT_REQUEST=2;
  public static final int PREFERENCES_REQUEST=3;
  public static final int CREATE_ACCOUNT_REQUEST=4;
  public static final int FILTER_CATEGORY_REQUEST=5;
  public static final int FILTER_COMMENT_REQUEST=6;
  public static final int TEMPLATE_TITLE_REQUEST=7;
  public static final int EDIT_SPLIT_REQUEST = 8;
  public static final int SELECT_CATEGORY_REQUEST = 9;
  public static final int EDIT_EVENT_REQUEST = 10;
  public static final int PICK_COLOR_REQUEST = 11;
  public static final int PICTURE_REQUEST_CODE = 14;
  public static final int IMPORT_FILENAME_REQUESTCODE = 15;
  public static final String SAVE_TAG = "SAVE_TASK";
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  private boolean scheduledRestart = false;
  public Enum<?> helpVariant = null;
  protected int colorExpense;
  public int getColorExpense() {
    return colorExpense;
  }

  protected int colorIncome;
  public int getColorIncome() {
    return colorIncome;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (BuildConfig.DEBUG) {
        enableStrictMode();
    }
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getSettings().registerOnSharedPreferenceChangeListener(this);
    setLanguage();
    Resources.Theme theme = getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
  }

  protected Toolbar setupToolbar(boolean withHome) {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (withHome) {
      final ActionBar actionBar = getSupportActionBar();
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    return toolbar;
  }

    @TargetApi(9)
    private void enableStrictMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
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
  }
  @Override
  @TargetApi(11)
  protected void onResume() {
    Log.d("DEBUG", "ProtectedFragmentActivity onResume");
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
    if (key.equals(MyApplication.PrefKey.UI_THEME_KEY.getKey()) ||
        key.equals(MyApplication.PrefKey.UI_LANGUAGE.getKey()) ||
        key.equals(MyApplication.PrefKey.UI_FONTSIZE.getKey())) {
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
    if (dispatchCommand(item.getItemId(),null)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (CommonCommands.dispatchCommand(this, command, tag)) {
      return true;
    }
    return false;
  }

  public void dispatchCommand(View v) {
    dispatchCommand(v.getId(),v.getTag());
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
    switch(taskId) {
    case TaskExecutionFragment.TASK_DELETE_TRANSACTION:
    case TaskExecutionFragment.TASK_DELETE_ACCOUNT:
    case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS:
    case TaskExecutionFragment.TASK_DELETE_CATEGORY:
    case TaskExecutionFragment.TASK_DELETE_PAYEES:
    case TaskExecutionFragment.TASK_DELETE_TEMPLATES:
      Boolean success = (Boolean) o;
      if (!success) {
        Toast.makeText(this, "There was an error deleting the object. Please contact support@myexenses.mobi !", Toast.LENGTH_LONG).show();
      }
      break;
    }
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
    t.remove(m.findFragmentByTag(SAVE_TAG));
    t.remove(m.findFragmentByTag(ProtectionDelegate.PROGRESS_TAG));
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
  public <T> void startTaskExecution(int taskId, T[] objectIds, Serializable extra, int progressMessage) {
    getProtection().startTaskExecution(taskId, objectIds, extra, progressMessage);
  }
  
  public void startDbWriteTask(boolean returnSequenceCount) {
    getSupportFragmentManager().beginTransaction()
    .add(DbWriteFragment.newInstance(returnSequenceCount), SAVE_TAG)
    .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_saving), ProtectionDelegate.PROGRESS_TAG)
    .commitAllowingStateLoss();
  }
  public void recordUsage(ContribFeature f) {
    f.recordUsage();
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
  /* 
   * @see android.support.v7.app.ActionBarActivity#onBackPressed()
   * https://code.google.com/p/android/issues/detail?id=25517
   */
  @Override
  public void onBackPressed() {
    try {
      super.onBackPressed();
    } catch (IllegalStateException e) {
      Utils.reportToAcra(e);
      finish();
    }
  }

  public void toggleCrStatus(View v) {
    Long id = (Long) v.getTag();
    if (id != -1) {
      startTaskExecution(
          TaskExecutionFragment.TASK_TOGGLE_CRSTATUS,
          new Long[] {id},
          null,
          0);
    }
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == ProtectionDelegate.CONTRIB_REQUEST) {
      if (resultCode == RESULT_OK) {
        ((ContribIFace) this).contribFeatureCalled(
            (ContribFeature) intent.getSerializableExtra(ContribInfoDialogActivity.KEY_FEATURE),
            intent.getSerializableExtra(ContribInfoDialogActivity.KEY_TAG));
      } else if (resultCode == RESULT_CANCELED) {
        ((ContribIFace) this).contribFeatureNotCalled(
            (ContribFeature) intent.getSerializableExtra(ContribInfoDialogActivity.KEY_FEATURE));
      }
    }
  }

  public void contribFeatureRequested(ContribFeature feature, Serializable tag) {
    if (feature.hasAccess()) {
      ((ContribIFace) this).contribFeatureCalled(feature, tag);
    }
    else {
      CommonCommands.showContribDialog(this,feature, tag);
    }
  }

  @Override
  public void setTitle(CharSequence title) {
    super.setTitle(title);
    if (Build.VERSION.SDK_INT <Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar().setTitle(title);
    }
  }
}
