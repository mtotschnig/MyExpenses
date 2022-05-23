package org.totschnig.myexpenses.activity;

import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;

import com.vmadalin.easypermissions.EasyPermissions;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.ContribUtils;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;
import org.totschnig.myexpenses.viewmodel.UpgradeHandlerViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import timber.log.Timber;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.CATEGORIES_SORT_BY_USAGES_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.CURRENT_VERSION;
import static org.totschnig.myexpenses.preference.PrefKey.HOME_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN;
import static org.totschnig.myexpenses.preference.PrefKey.SHARE_TARGET;
import static org.totschnig.myexpenses.preference.PrefKey.SORT_ORDER_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.SYNC_UPSELL_NOTIFICATION_SHOWN;
import static org.totschnig.myexpenses.preference.PreferenceUtilsKt.enableAutoFill;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public abstract class LaunchActivity extends IapActivity {

  @Override
  public boolean getShouldQueryIap() {
    return true;
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
                ExtendProLicenceDialogFragment.Companion.newInstance(message).show(getSupportFragmentManager(), "UP_SELL");
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

  @Override
  public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    super.onPermissionsDenied(requestCode, perms);
    if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR && EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      requireApplication().removePlanner();
    }
  }

  @Override
  public void onBillingSetupFailed(@NonNull String reason) {
    LicenceHandler.Companion.log().w("Billing setup failed (%s)", reason);
  }

  @Override
  public void onLicenceStatusSet(String newStatus) {
    if (newStatus != null) {
      showSnackBar(getString(R.string.licence_validation_premium) + " (" + newStatus + ")");
    } else {
      showSnackBar(R.string.licence_validation_failure);
    }
  }
}
