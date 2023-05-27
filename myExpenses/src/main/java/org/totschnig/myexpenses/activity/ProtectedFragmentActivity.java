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

import static org.totschnig.myexpenses.activity.ConstantsKt.CALCULATOR_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.CONTRIB_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.PREFERENCES_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ContribInfoDialogActivity.KEY_FEATURE;
import static org.totschnig.myexpenses.preference.PrefKey.CUSTOM_DATE_FORMAT;
import static org.totschnig.myexpenses.preference.PrefKey.DB_SAFE_MODE;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_MONTH_STARTS;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_WEEK_STARTS;
import static org.totschnig.myexpenses.preference.PrefKey.HOME_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.UI_FONTSIZE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.util.MoreUiUtilsKt.setBackgroundTintList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.evernote.android.state.StateSaver;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.util.ColorUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.inject.Inject;
public abstract class ProtectedFragmentActivity extends BaseActivity
    implements OnSharedPreferenceChangeListener,
    TaskExecutionFragment.TaskCallbacks, DbWriteFragment.TaskCallbacks,
    ProgressDialogFragment.ProgressDialogListener {

  public static final String SAVE_TAG = "SAVE_TASK";
  public static final int RESULT_RESTORE_OK = RESULT_FIRST_USER + 1;
  public static final String EDIT_COLOR_DIALOG = "editColorDialog";

  protected ColorStateList textColorSecondary;

  @Inject
  protected CurrencyContext currencyContext;

  @Inject
  protected CurrencyFormatter currencyFormatter;

  @Inject
  protected SharedPreferences settings;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    StateSaver.restoreInstanceState(this, savedInstanceState);
    if (requireApplication().isProtected()) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
          WindowManager.LayoutParams.FLAG_SECURE);
    }
    settings.registerOnSharedPreferenceChangeListener(this);
    TypedArray themeArray = getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
    textColorSecondary = themeArray.getColorStateList(0);
  }

  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    StateSaver.saveInstanceState(this, outState);
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(newBase);
    final MyApplication application = MyApplication.getInstance();
    final int customFontScale = application.getAppComponent().prefHandler().getInt(UI_FONTSIZE, 0);
    if (customFontScale > 0) {
      Configuration config = new Configuration();
      config.fontScale = getFontScale(customFontScale, application.getContentResolver());
      applyOverrideConfiguration(config);
    }
    featureManager.initActivity(this);
  }

  private float getFontScale(int customFontScale, ContentResolver contentResolver) {
    return Settings.System.getFloat(contentResolver, Settings.System.FONT_SCALE, 1.0f) * (1 + customFontScale / 10F);
  }

  @Override
  protected void injectDependencies() {
    ((MyApplication) getApplicationContext()).getAppComponent().inject(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    settings.unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    getCrashHandler().addBreadcrumb(getClass().getSimpleName());
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    if (prefHandler.matches(key, UI_FONTSIZE, PROTECTION_LEGACY, DB_SAFE_MODE,
        PROTECTION_DEVICE_LOCK_SCREEN, GROUP_MONTH_STARTS, GROUP_WEEK_STARTS, HOME_CURRENCY, CUSTOM_DATE_FORMAT)) {
      setScheduledRestart(true);
    }
  }

  @Override
  public void onProgressDialogDismiss() {
  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    super.onCreateOptionsMenu(menu);
    inflateHelpMenu(menu);
    return true;
  }

  protected void inflateHelpMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.help, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId != 0) {
      //isChecked reports the value at the moment the item is clicked, we dispatch command with the
      //requested new value
      if (dispatchCommand(itemId, item.isCheckable() ? !item.isChecked() : null)) {
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPreExecute() {
  }

  @Override
  public void onProgressUpdate(Object progress) {
    FragmentManager m = getSupportFragmentManager();
    ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag(PROGRESS_TAG));
    if (f != null) {
      if (progress instanceof Integer) {
        f.setProgress((Integer) progress);
      } else if (progress instanceof String) {
        f.appendToMessage((String) progress);
      } else if (progress instanceof Result) {
        String print = ((Result) progress).print0(this);
        if (print != null) {
          f.appendToMessage(print);
        }
      }
    }
  }

  @Override
  public void onCancelled() {
    removeAsyncTaskFragment(false);
  }

  protected boolean shouldKeepProgress(int taskId) {
    return false;
  }

  public void tintSystemUiAndFab(int color) {
    tintSystemUi(color);
    setBackgroundTintList(getFloatingActionButton(), color);
  }

  public void tintSystemUi(int color) {
    if (shouldTintSystemUi()) {
      Window window = getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      int color700 = ColorUtils.get700Tint(color);
      window.setStatusBarColor(color700);
      window.setNavigationBarColor(color700);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        window.getDecorView().setSystemUiVisibility(
            ColorUtils.isBrightColor(color700) ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          window.getDecorView().setSystemUiVisibility(
              ColorUtils.isBrightColor(color700) ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        }
      }
    }
  }

  public boolean shouldTintSystemUi() {
    try {
      //on DialogWhenLargeTheme we do not want to tint if we are displayed on a large screen as dialog
      return getPackageManager().getActivityInfo(getComponentName(), 0).getThemeResource() != R.style.EditDialog ||
          (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) <
              Configuration.SCREENLAYOUT_SIZE_LARGE;
    } catch (PackageManager.NameNotFoundException e) {
      CrashHandler.report(e);
      return false;
    }
  }

  @Override
  public void onPostExecute(int taskId, @Nullable Object o) {
    removeAsyncTaskFragment(shouldKeepProgress(taskId));
    switch (taskId) {
      case TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS: {
        Result result = (Result) o;
        if (!result.isSuccess()) {
          showDeleteFailureFeedback(null, null);
        }
        break;
      }
    }
  }

  @Override
  public Model getObject() {
    return null;
  }

  @Override
  public void onPostExecute(@Nullable Uri result) {
    FragmentManager m = getSupportFragmentManager();
    FragmentTransaction t = m.beginTransaction();
    t.remove(m.findFragmentByTag(SAVE_TAG));
    t.remove(m.findFragmentByTag(PROGRESS_TAG));
    t.commitAllowingStateLoss();
  }

  /**
   * starts the given task, only if no task is currently executed,
   * informs user through snackbar in that case
   *
   * @param progressMessage if 0 no progress dialog will be shown
   */
  @Deprecated
  public <T> void startTaskExecution(int taskId, T[] objectIds, Serializable extra,
                                     int progressMessage) {
    startTaskExecution(taskId, objectIds, extra, progressMessage, false);
  }

  @Deprecated
  public <T> void startTaskExecution(int taskId, T[] objectIds, Serializable extra,
                                     int progressMessage, boolean withButton) {
    FragmentManager m = getSupportFragmentManager();
    if (hasPendingTask()) {
      return;
    }
    if (m.isStateSaved()) {
      return;
    }
    //noinspection AndroidLintCommitTransaction
    FragmentTransaction ft = m.beginTransaction()
        .add(TaskExecutionFragment.newInstance(
            taskId,
            objectIds, extra),
            ASYNC_TAG);
    if (progressMessage != 0) {
      ft.add(ProgressDialogFragment.newInstance(getString(progressMessage), withButton), PROGRESS_TAG);
    }
    ft.commit();
  }

  private boolean hasPendingTask() {
    FragmentManager m = getSupportFragmentManager();
    final boolean result = m.findFragmentByTag(ASYNC_TAG) != null;
    if (result) {
      showSnackBar("Previous task still executing, please try again later");
    }
    return result;
  }

  public void startTaskExecution(int taskId, @NonNull Bundle extras, int progressMessage) {
    FragmentManager m = getSupportFragmentManager();
    if (hasPendingTask()) {
      return;
    }
    //noinspection AndroidLintCommitTransaction
    FragmentTransaction ft = m.beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(extras, taskId),
            ASYNC_TAG);
    if (progressMessage != 0) {
      ft.add(ProgressDialogFragment.newInstance(getString(progressMessage)), PROGRESS_TAG);
    }
    ft.commit();
  }

  private void removeAsyncTaskFragment(boolean keepProgress) {
    FragmentManager m = getSupportFragmentManager();
    FragmentTransaction t = m.beginTransaction();
    ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag(PROGRESS_TAG));
    if (f != null) {
      if (keepProgress) {
        f.onTaskCompleted();
      } else {
        t.remove(f);
      }
    }
    final Fragment asyncFragment = m.findFragmentByTag(ASYNC_TAG);
    if (asyncFragment != null) {
      t.remove(asyncFragment);
    }
    t.commitAllowingStateLoss();
    //we might want to call a new task immediately after executing the last one
    m.executePendingTransactions();
  }

  @Deprecated
  public void startDbWriteTask() {
    getSupportFragmentManager().beginTransaction()
        .add(DbWriteFragment.newInstance(), SAVE_TAG)
        .add(ProgressDialogFragment.newInstance(getString(R.string.saving)),
            PROGRESS_TAG)
        .commitAllowingStateLoss();
  }

  public void recordUsage(ContribFeature f) {
    f.recordUsage(prefHandler, licenceHandler);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == CONTRIB_REQUEST && intent != null) {
      ContribFeature contribFeature = ContribFeature.valueOf(intent.getStringExtra(KEY_FEATURE));
      if (resultCode == RESULT_OK) {
        ((ContribIFace) this).contribFeatureCalled(contribFeature,
            intent.getSerializableExtra(ContribInfoDialogActivity.KEY_TAG));
      } else if (resultCode == RESULT_CANCELED) {
        ((ContribIFace) this).contribFeatureNotCalled(contribFeature);
      }
    }
    if ((requestCode == PREFERENCES_REQUEST || requestCode == RESTORE_REQUEST) && resultCode == RESULT_RESTORE_OK) {
      restartAfterRestore();
    }
    if (resultCode == RESULT_OK && requestCode == CALCULATOR_REQUEST && intent != null) {
      View target = findViewById(intent.getIntExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, 0));
      if (target instanceof AmountInput) {
        ((AmountInput) target).setAmount(new BigDecimal(intent.getStringExtra(KEY_AMOUNT)), false);
      } else {
        showSnackBar("CALCULATOR_REQUEST launched with incorrect EXTRA_KEY_INPUT_ID");
      }
    }
  }

  protected void restartAfterRestore() {
    ((MyApplication) getApplication()).invalidateHomeCurrency(homeCurrencyProvider.getHomeCurrencyString());
    if (!isFinishing()) {
      Intent i = new Intent(this, MyExpenses.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      finishAffinity();
      startActivity(i);
    }
  }

  public void checkGdprConsent(boolean forceShow) {
    adHandlerFactory.gdprConsent(this, forceShow);
  }

  @Override
  public void onCurrencySelectionChanged(CurrencyUnit currencyUnit) {
  }

  @Override
  public CurrencyContext getCurrencyContext() {
    return currencyContext;
  }

  public CurrencyFormatter getCurrencyFormatter() {
    return currencyFormatter;
  }

  public enum ThemeType {
    dark, light
  }
}
