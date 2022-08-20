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

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.annimon.stream.Optional;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.feature.FeatureManager;
import org.totschnig.myexpenses.fragment.DbWriteFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.service.DailyScheduler;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.util.ColorUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.ads.AdHandlerFactory;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;
import org.totschnig.myexpenses.widget.AbstractWidgetKt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import icepick.Icepick;

import static org.totschnig.myexpenses.MyApplication.FEEDBACK_EMAIL;
import static org.totschnig.myexpenses.activity.ConstantsKt.CALCULATOR_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.CONTRIB_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.PREFERENCES_REQUEST;
import static org.totschnig.myexpenses.activity.ConstantsKt.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ContribInfoDialogActivity.KEY_FEATURE;
import static org.totschnig.myexpenses.preference.PrefKey.CRITERION_FUTURE;
import static org.totschnig.myexpenses.preference.PrefKey.CUSTOM_DATE_FORMAT;
import static org.totschnig.myexpenses.preference.PrefKey.DB_SAFE_MODE;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_MONTH_STARTS;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_WEEK_STARTS;
import static org.totschnig.myexpenses.preference.PrefKey.HOME_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.UI_FONTSIZE;
import static org.totschnig.myexpenses.preference.PrefKey.UI_LANGUAGE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;
import static org.totschnig.myexpenses.util.distrib.DistributionHelper.getMarketSelfUri;
import static org.totschnig.myexpenses.util.distrib.DistributionHelper.getVersionInfo;

public abstract class ProtectedFragmentActivity extends BaseActivity
    implements OnSharedPreferenceChangeListener,
    ConfirmationDialogFragment.ConfirmationDialogListener,
    TaskExecutionFragment.TaskCallbacks, DbWriteFragment.TaskCallbacks,
    ProgressDialogFragment.ProgressDialogListener, AmountInput.Host {

  public static final String SAVE_TAG = "SAVE_TASK";
  public static final int RESULT_RESTORE_OK = RESULT_FIRST_USER + 1;
  public static final String EDIT_COLOR_DIALOG = "editColorDialog";

  public static final String ASYNC_TAG = "ASYNC_TASK";
  public static final String PROGRESS_TAG = "PROGRESS";

  private AlertDialog pwDialog;
  private boolean scheduledRestart = false;
  private Optional<Boolean> confirmCredentialResult = Optional.empty();
  protected ColorStateList textColorSecondary;
  @Nullable
  protected FloatingActionButton floatingActionButton;

  @Inject
  protected AdHandlerFactory adHandlerFactory;

  @Inject
  protected LicenceHandler licenceHandler;

  @Inject
  protected CurrencyContext currencyContext;

  @Inject
  protected CurrencyFormatter currencyFormatter;

  @Inject
  protected SharedPreferences settings;

  @Inject
  protected FeatureManager featureManager;

  private Pair<Integer, Integer> focusAfterRestoreInstanceState;

  public ColorStateList getTextColorSecondary() {
    return textColorSecondary;
  }

  MyApplication requireApplication() {
    return ((MyApplication) getApplication());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Icepick.restoreInstanceState(this, savedInstanceState);
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
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(newBase);
    final MyApplication application = MyApplication.getInstance();
    final int customFontScale = application.getAppComponent().prefHandler().getInt(UI_FONTSIZE, 0);
    if (customFontScale > 0 || !application.getAppComponent().userLocaleProvider().getPreferredLanguage().equals(MyApplication.DEFAULT_LANGUAGE)) {
      Configuration config = new Configuration();
      config.fontScale = getFontScale(customFontScale, application.getContentResolver());
      applyOverrideConfiguration(config);
    }
    featureManager.initActivity(this);
  }

  @Override
  public void applyOverrideConfiguration(Configuration newConfig) {
    super.applyOverrideConfiguration(updateConfigurationIfSupported(newConfig));
  }

  private Configuration updateConfigurationIfSupported(Configuration config) {
    final UserLocaleProvider userLocaleProvider = MyApplication.getInstance().getAppComponent().userLocaleProvider();
    if (userLocaleProvider.getPreferredLanguage().equals(MyApplication.DEFAULT_LANGUAGE)) {
      return config;
    }
    // Configuration.getLocales is added after 24 and Configuration.locale is deprecated in 24
    if (Build.VERSION.SDK_INT >= 24) {
      if (!config.getLocales().isEmpty()) {
        return config;
      }
    } else {
      if (config.locale != null) {
        return config;
      }
    }

    config.setLocale(userLocaleProvider.getUserPreferredLocale());
    return config;
  }

  private float getFontScale(int customFontScale, ContentResolver contentResolver) {
    return Settings.System.getFloat(contentResolver, Settings.System.FONT_SCALE, 1.0f) * (1 + customFontScale / 10F);
  }

  @Override
  protected void injectDependencies() {
    ((MyApplication) getApplicationContext()).getAppComponent().inject(this);
  }

  protected void configureFloatingActionButton(int fabDescription) {
    configureFloatingActionButton(fabDescription, 0);
  }

  protected void configureFloatingActionButton(int fabDescription, int icon) {
    if (!requireFloatingActionButtonWithContentDescription(getString(fabDescription))) return;
    if (icon != 0) {
      floatingActionButton.setImageResource(icon);
    }
  }

  protected boolean requireFloatingActionButton() {
    floatingActionButton = findViewById(R.id.CREATE_COMMAND);
    return floatingActionButton != null;
  }

  protected boolean requireFloatingActionButtonWithContentDescription(String fabDescription) {
    boolean found = requireFloatingActionButton();
    if (found) {
      floatingActionButton.setContentDescription(fabDescription);
    }
    return found;
  }

  protected Toolbar setupToolbar(boolean withHome) {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (withHome) {
      final ActionBar actionBar = getSupportActionBar();
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    return toolbar;
  }

  @Override
  protected void onPause() {
    super.onPause();
    MyApplication app = requireApplication();
    if (app.isLocked() && pwDialog != null) {
      pwDialog.dismiss();
    } else {
      app.setLastPause(this);
    }
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
    if (scheduledRestart) {
      scheduledRestart = false;
      recreate();
    } else {
      if (confirmCredentialResult.isPresent()) {
        if (!confirmCredentialResult.get()) {
          moveTaskToBack(true);
        }
        confirmCredentialResult = Optional.empty();
      } else {
        MyApplication app = requireApplication();
        if (app.shouldLock(this)) {
          confirmCredentials(CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST, null, true);
        }
      }
    }
  }

  protected void confirmCredentials(int requestCode, DialogUtils.PasswordDialogUnlockedCallback legacyUnlockCallback, boolean shouldHideWindow) {
    if (prefHandler.getBoolean(PROTECTION_DEVICE_LOCK_SCREEN, false)) {
      Intent intent = ((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE))
          .createConfirmDeviceCredentialIntent(null, null);
      if (intent != null) {
        if (shouldHideWindow) hideWindow();
        try {
          startActivityForResult(intent, requestCode);
          requireApplication().setLocked(true);
        } catch (ActivityNotFoundException e) {
          showSnackBar("No activity found for confirming device credentials");
        }
      } else {
        showDeviceLockScreenWarning();
        if (legacyUnlockCallback != null) {
          legacyUnlockCallback.onPasswordDialogUnlocked();
        }
      }
    } else if (prefHandler.getBoolean(PROTECTION_LEGACY, true)) {
      if (shouldHideWindow) hideWindow();
      if (pwDialog == null) {
        pwDialog = DialogUtils.passwordDialog(this, false);
      }
      DialogUtils.showPasswordDialog(this, pwDialog, legacyUnlockCallback);
      requireApplication().setLocked(true);
    }
  }

  public void showDeviceLockScreenWarning() {
    showSnackBar(
        concatResStrings(this, " ", R.string.warning_device_lock_screen_not_set_up_1, R.string.warning_device_lock_screen_not_set_up_2));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    if (prefHandler.matches(key, UI_LANGUAGE, UI_FONTSIZE, PROTECTION_LEGACY, DB_SAFE_MODE,
        PROTECTION_DEVICE_LOCK_SCREEN, GROUP_MONTH_STARTS, GROUP_WEEK_STARTS, HOME_CURRENCY, CUSTOM_DATE_FORMAT, CRITERION_FUTURE)) {
      userLocaleProvider.invalidate();
      scheduledRestart = true;
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
  public boolean dispatchCommand(int command, @Nullable Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    Intent i;
    if (command == R.id.RATE_COMMAND) {
      i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(getMarketSelfUri()));
      startActivity(i, R.string.error_accessing_market, null);
      return true;
    } else if (command == R.id.SETTINGS_COMMAND) {
      i = new Intent(this, MyPreferenceActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      if (tag != null) {
        i.putExtra(MyPreferenceActivity.KEY_OPEN_PREF_KEY, (String) tag);
      }
      startActivityForResult(i, PREFERENCES_REQUEST);
      return true;
    } else if (command == R.id.FEEDBACK_COMMAND) {
      LicenceStatus licenceStatus = licenceHandler.getLicenceStatus();
      String licenceInfo = "";
      if (licenceStatus != null) {
        licenceInfo = "\nLICENCE: " + licenceStatus.name();
        String purchaseExtraInfo = licenceHandler.getPurchaseExtraInfo();
        if (!TextUtils.isEmpty(purchaseExtraInfo)) {
          licenceInfo += " (" + purchaseExtraInfo + ")";
        }
      }
      sendEmail(FEEDBACK_EMAIL,
              "[" + getString(R.string.app_name) + "] " + getString(R.string.feedback),
              String.format(Locale.ROOT,
                      "APP_VERSION:%s\nFIRST_INSTALL_VERSION:%d (DB_SCHEMA %d)\nANDROID_VERSION:%s\nBRAND:%s\nMODEL:%s\nCONFIGURATION:%s%s\n\n",
                      getVersionInfo(this),
                      prefHandler.getInt(PrefKey.FIRST_INSTALL_VERSION, 0),
                      prefHandler.getInt(PrefKey.FIRST_INSTALL_DB_SCHEMA_VERSION, -1),
                      Build.VERSION.RELEASE,
                      Build.BRAND,
                      Build.MODEL,
                      ConfigurationHelper.configToJson(getResources().getConfiguration()),
                      licenceInfo),
              null
      );
    } else if (command == R.id.CONTRIB_INFO_COMMAND) {
      showContribDialog(null, null);
      return true;
    } else if (command == R.id.WEB_COMMAND) {
      startActionView(getString(R.string.website));
      return true;
    } else if (command == R.id.HELP_COMMAND) {
      return doHelp((String) tag);
    } else if (command == android.R.id.home) {
      doHome();
      return true;
    } else if (command == R.id.GDPR_CONSENT_COMMAND) {
      adHandlerFactory.setConsent(this, (Boolean) tag);
      return true;
    } else if (command == R.id.GDPR_NO_CONSENT_COMMAND) {
      adHandlerFactory.clearConsent();
      contribFeatureRequested(ContribFeature.AD_FREE, null);
      return true;
    }
    return false;
  }

  public void showContribDialog(@Nullable ContribFeature feature, @Nullable Serializable tag) {
    Intent i = ContribInfoDialogActivity.Companion.getIntentFor(this, feature);
    i.putExtra(ContribInfoDialogActivity.KEY_TAG, tag);
    startActivityForResult(i, CONTRIB_REQUEST);
  }

  protected boolean doHelp(String variant) {
    Intent i;
    i = new Intent(this, Help.class);
    i.putExtra(HelpDialogFragment.KEY_CONTEXT, this.getClass().getSimpleName());
    i.putExtra(HelpDialogFragment.KEY_VARIANT,
        variant != null ? variant : getHelpVariant());
    //for result is needed since it allows us to inspect the calling activity
    startActivity(i);
    return true;
  }

  protected void doHome() {
    setResult(FragmentActivity.RESULT_CANCELED);
    finish();
  }

  public void dispatchCommand(View v) {
    dispatchCommand(v.getId(), v.getTag());
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
    UiUtils.setBackgroundTintListOnFab(floatingActionButton, color);
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

  /*
   * @see android.support.v7.app.ActionBarActivity#onBackPressed()
   * https://code.google.com/p/android/issues/detail?id=25517
   */
  @Override
  public void onBackPressed() {
    try {
      super.onBackPressed();
    } catch (IllegalStateException e) {
      CrashHandler.report(e);
      finish();
    }
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
    if (requestCode == CONFIRM_DEVICE_CREDENTIALS_UNLOCK_REQUEST) {
      if (resultCode == RESULT_OK) {
        confirmCredentialResult = Optional.of(true);
        showWindow();
        requireApplication().setLocked(false);
      } else {
        confirmCredentialResult = Optional.of(false);
      }
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
    invalidateHomeCurrency();
    if (!isFinishing()) {
      Intent i = new Intent(this, MyExpenses.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      finishAffinity();
      startActivity(i);
    }
  }

  public void contribFeatureRequested(@NonNull ContribFeature feature, @Nullable Serializable tag) {
    if (licenceHandler.hasAccessTo(feature)) {
      ((ContribIFace) this).contribFeatureCalled(feature, tag);
    } else {
      showContribDialog(feature, tag);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (floatingActionButton != null) {
      floatingActionButton.setEnabled(true);
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
    if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
      DailyScheduler.updatePlannerAlarms(this, false, true);
    }
  }

  public void requestCalendarPermission() {
    requestPermission(PermissionGroup.CALENDAR);
  }

  @Override
  public void requestPermission(@NonNull PermissionGroup permissionGroup) {
    if (floatingActionButton != null) {
      floatingActionButton.setEnabled(false);
    }
    super.requestPermission(permissionGroup);
  }

  @Override
  public void onPositive(Bundle args, boolean checked) {
    dispatchCommand(args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE),
        args.getSerializable(ConfirmationDialogFragment.KEY_TAG_POSITIVE));
  }

  @Override
  public void onNegative(Bundle args) {
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
  }

  @VisibleForTesting
  @Nullable
  public Fragment getCurrentFragment() {
    return null;
  }

  public void hideWindow() {
    findViewById(android.R.id.content).setVisibility(View.GONE);
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) actionBar.hide();
  }

  public void showWindow() {
    findViewById(android.R.id.content).setVisibility(View.VISIBLE);
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) actionBar.show();
  }

  public void checkGdprConsent(boolean forceShow) {
    adHandlerFactory.gdprConsent(this, forceShow);
  }

  public void invalidateHomeCurrency() {
    currencyContext.invalidateHomeCurrency();
    currencyFormatter.invalidate(AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE, getContentResolver());
    Transaction.buildProjection(this);
    Account.buildProjection();
    getContentResolver().notifyChange(TransactionProvider.TRANSACTIONS_URI, null, false);
  }

  public void showCalculator(BigDecimal amount, int id) {
    Intent intent = new Intent(this, CalculatorInput.class);
    forwardDataEntryFromWidget(intent);
    if (amount != null) {
      intent.putExtra(KEY_AMOUNT, amount);
    }
    intent.putExtra(CalculatorInput.EXTRA_KEY_INPUT_ID, id);
    startActivityForResult(intent, CALCULATOR_REQUEST);
  }

  protected void forwardDataEntryFromWidget(Intent intent) {
    intent.putExtra(AbstractWidgetKt.EXTRA_START_FROM_WIDGET_DATA_ENTRY,
        getIntent().getBooleanExtra(AbstractWidgetKt.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false));
  }

  @Override
  public void setFocusAfterRestoreInstanceState(Pair<Integer, Integer> focusView) {
    this.focusAfterRestoreInstanceState = focusView;
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

  @Override
  protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (focusAfterRestoreInstanceState != null) {
      findViewById(focusAfterRestoreInstanceState.first).findViewById(focusAfterRestoreInstanceState.second).requestFocus();
    }
  }

  public enum ThemeType {
    dark, light
  }
}
