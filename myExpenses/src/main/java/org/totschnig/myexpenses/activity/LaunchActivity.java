package org.totschnig.myexpenses.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment;
import org.totschnig.myexpenses.dialog.VersionDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.ContribUtils;
import org.totschnig.myexpenses.util.DistributionHelper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.BillingListener;
import org.totschnig.myexpenses.util.licence.BillingManager;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import timber.log.Timber;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.CATEGORIES_SORT_BY_USAGES_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.CURRENT_VERSION;
import static org.totschnig.myexpenses.preference.PrefKey.HOME_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.LICENCE_MIGRATION_INFO_SHOWN;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN;
import static org.totschnig.myexpenses.preference.PrefKey.SHARE_TARGET;
import static org.totschnig.myexpenses.preference.PrefKey.SORT_ORDER_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN;
import static org.totschnig.myexpenses.preference.PreferenceUtilsKt.enableAutoFill;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public abstract class LaunchActivity extends ProtectedFragmentActivity implements BillingListener {

  public static final String TAG_VERSION_INFO = "VERSION_INFO";
  private BillingManager billingManager;
  private UpgradeHandlerViewModel upgradeHandlerViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    billingManager = licenceHandler.initBillingManager(this, true);

    upgradeHandlerViewModel = ViewModelProviders.of(this).get(UpgradeHandlerViewModel.class);
  }

  @Override
  public boolean dispatchCommand(int command, @Nullable Object tag) {
    if (command == R.id.QUIT_COMMAND) {
      finish();
      return true;
    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    if (savedInstanceState == null) {
      if (DistributionHelper.isGithub()) {
        if (licenceHandler.getLicenceStatus() != null) {
          final long now = System.currentTimeMillis();
          if (licenceHandler.getLicenceStatus() == LicenceStatus.PROFESSIONAL) {
            long licenceValidity = licenceHandler.getValidUntilMillis();
            if (licenceValidity != 0) {
              final long daysToGo = TimeUnit.MILLISECONDS.toDays(licenceValidity - now);
              if (daysToGo <= 7 && (now -
                  prefHandler.getLong(PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN, 0)
                  > DAY_IN_MILLIS)) {
                String message;
                if (daysToGo > 1) {
                  message = getString(R.string.licence_expires_n_days, daysToGo);
                } else if (daysToGo == 1) {
                  message = getString(R.string.licence_expires_tomorrow);
                } else if (daysToGo == 0) {
                  message = getString(R.string.licence_expires_today);
                } else if (daysToGo == -1) {
                  message = getString(R.string.licence_expired_yesterday);
                } else {
                  if (daysToGo < -7) {//grace period is over,
                    licenceHandler.handleExpiration();
                  }
                  message = getString(R.string.licence_has_expired_n_days, -daysToGo);
                }
                prefHandler.putLong(PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN, now);
                ExtendProLicenceDialogFragment.Companion.newInstance(message).show(getSupportFragmentManager(), "UPSELL");
              }
            }
          }
        }
      }
    }
    if (licenceHandler.getLicenceStatus() == null) {
      checkGdprConsent(false);
    }
  }

  /**
   * check if this is the first invocation of a new version
   * in which case help dialog is presented
   * also is used for hooking version specific upgrade procedures
   * and display information to be presented upon app launch
   */
  public void newVersionCheck() {
    int prev_version = prefHandler.getInt(CURRENT_VERSION, -1);
    int current_version = DistributionHelper.getVersionNumber();
    if (prev_version < current_version) {
      if (prev_version == -1) {
        return;
      }
      upgradeHandlerViewModel.upgrade(prev_version, current_version);
      boolean showImportantUpgradeInfo = false;
      prefHandler.putInt(CURRENT_VERSION, current_version);
      SharedPreferences settings = MyApplication.getInstance().getSettings();
      Editor edit = settings.edit();
      if (prev_version < 19) {
        edit.putString(prefHandler.getKey(SHARE_TARGET), settings.getString("ftp_target", ""));
        edit.remove("ftp_target");
        edit.apply();
      }
      if (prev_version < 28) {
        Timber.i("Upgrading to version 28: Purging %d transactions from datbase",
            getContentResolver().delete(TransactionProvider.TRANSACTIONS_URI,
                KEY_ACCOUNTID + " not in (SELECT _id FROM accounts)", null));
      }
      if (prev_version < 30) {
        if (!"".equals(prefHandler.getString(SHARE_TARGET, ""))) {
          edit.putBoolean(prefHandler.getKey(SHARE_TARGET), true);
          edit.apply();
        }
      }
      if (prev_version < 40) {
        //this no longer works since we migrated time to utc format
        //  DbUtils.fixDateValues(getContentResolver());
        //we do not want to show both reminder dialogs too quickly one after the other for upgrading users
        //if they are already above both tresholds, so we set some delay
        edit.putLong("nextReminderContrib", Transaction.getSequenceCount() + 23);
        edit.apply();
      }
      if (prev_version < 163) {
        edit.remove("qif_export_file_encoding");
        edit.apply();
      }
      if (prev_version < 199) {
        //filter serialization format has changed
        for (Map.Entry<String, ?> entry : settings.getAll().entrySet()) {
          String key = entry.getKey();
          String[] keyParts = key.split("_");
          if (keyParts[0].equals("filter")) {
            String val = settings.getString(key, "");
            switch (keyParts[1]) {
              case "method":
              case "payee":
              case "cat":
                int sepIndex = val.indexOf(";");
                edit.putString(key, val.substring(sepIndex + 1) + ";" + Criteria.escapeSeparator(val.substring(0, sepIndex)));
                break;
              case "cr":
                edit.putString(key, CrStatus.values()[Integer.parseInt(val)].name());
                break;
            }
          }
        }
        edit.apply();
      }
      if (prev_version < 202) {
        String appDir = prefHandler.getString(APP_DIR, null);
        if (appDir != null) {
          prefHandler.putString(APP_DIR, Uri.fromFile(new File(appDir)).toString());
        }
      }
      if (prev_version < 221) {
        prefHandler.putString(SORT_ORDER_LEGACY,
            prefHandler.getBoolean(CATEGORIES_SORT_BY_USAGES_LEGACY, true) ?
                "USAGES" : "ALPHABETIC");
      }
      if (prev_version < 303) {
        if (prefHandler.getBoolean(AUTO_FILL_LEGACY, false)) {
          enableAutoFill(prefHandler);
        }
        prefHandler.remove(AUTO_FILL_LEGACY);
      }
      if (prev_version < 316) {
        prefHandler.putString(HOME_CURRENCY, Utils.getHomeCurrency().code());
        invalidateHomeCurrency();
      }
      if (prev_version < 354) {
        showImportantUpgradeInfo = GenericAccountService.getAccountsAsArray(this).length > 0;
      }

      VersionDialogFragment.newInstance(prev_version, showImportantUpgradeInfo)
          .show(getSupportFragmentManager(), TAG_VERSION_INFO);
    } else {
      if (licenceHandler.needsMigration() &&
          !prefHandler.getBoolean(LICENCE_MIGRATION_INFO_SHOWN, false)) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(
            ConfirmationDialogFragment.KEY_MESSAGE,
            Utils.getTextWithAppName(this, R.string.licence_migration_info));
        bundle.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
            R.id.REQUEST_LICENCE_MIGRATION_COMMAND);
        bundle.putString(ConfirmationDialogFragment.KEY_PREFKEY,
            LICENCE_MIGRATION_INFO_SHOWN.getKey());
        bundle.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.pref_request_licence_title);
        ConfirmationDialogFragment.newInstance(bundle).show(getSupportFragmentManager(),
            "RESTORE");
      }
      if (!ContribFeature.SYNCHRONIZATION.hasAccess() && ContribFeature.SYNCHRONIZATION.usagesLeft(prefHandler) < 1 &&
          !prefHandler.getBoolean(SYNC_UPSELL_NOTIFICATION_SHOWN, false)) {
        prefHandler.putBoolean(SYNC_UPSELL_NOTIFICATION_SHOWN, true);
        ContribUtils.showContribNotification(this, ContribFeature.SYNCHRONIZATION);
      }
    }
    checkCalendarPermission();
  }

  private void checkCalendarPermission() {
    if (!prefHandler.getString(PLANNER_CALENDAR_ID, "-1").equals("-1")) {
      if (!CALENDAR.hasPermission(this)) {
        requestPermission(CALENDAR);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR:
        if (!PermissionHelper.allGranted(grantResults)) {
          if (!CALENDAR.shouldShowRequestPermissionRationale(this)) {
            MyApplication.getInstance().removePlanner();
          }
        }
        break;
    }
  }

  @Override
  public void onBillingSetupFinished() {

  }

  @Override
  public void onBillingSetupFailed(@NonNull String reason) {
    CrashHandler.reportWithTag(String.format("Billing setup failed (%s)", reason), LicenceHandler.TAG);
  }

  @Override
  public void onLicenceStatusSet(@Nullable LicenceStatus newStatus, @Nullable LicenceStatus oldStatus) {
    if (newStatus != oldStatus) {
      if (newStatus != null) {
        showMessage(String.format("%s (%s)", getString(R.string.licence_validation_premium),
            getString(newStatus.getResId())));
      } else {
        showSnackbar(R.string.licence_validation_failure, Snackbar.LENGTH_LONG);
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (billingManager != null) {
      billingManager.destroy();
    }
    billingManager = null;
  }
}
