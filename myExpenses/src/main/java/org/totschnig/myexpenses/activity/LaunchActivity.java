package org.totschnig.myexpenses.activity;

import android.os.Bundle;

import com.vmadalin.easypermissions.EasyPermissions;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ExtendProLicenceDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static org.totschnig.myexpenses.preference.PrefKey.PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN;

public abstract class LaunchActivity extends IapActivity {

  @Override
  public boolean getShouldQueryIap() {
    return true;
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
  }

  @Override
  public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    super.onPermissionsDenied(requestCode, perms);
    if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR && EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      requireApplication().removePlanner();
    }
  }

  @Override
  public void onBillingSetupFinished() {
    if (!licenceHandler.hasAccessTo(ContribFeature.AD_FREE)) {
      checkGdprConsent(false);
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
