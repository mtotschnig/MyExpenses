package org.totschnig.myexpenses.util;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.licence.Licence;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import java.util.Date;

public class LicenceHandler {
  private static final String LICENSE_STATUS_KEY = "licence_status";
  private static final String LICENSE_VALID_SINCE_KEY = "licence_valid_since";
  private static final String LICENSE_VALID_UNTIL_KEY = "licence_valid_until";
  public static boolean HAS_EXTENDED = !DistribHelper.isBlackberry();
  public static LicenceStatus EXTENDED = HAS_EXTENDED ? LicenceStatus.EXTENDED : LicenceStatus.CONTRIB;
  protected final Context context;

  public LicenceStatus getLicenceStatus() {
    return licenceStatus;
  }

  protected LicenceStatus licenceStatus;
  PreferenceObfuscator licenseStatusPrefs;

  protected LicenceHandler(Context context, PreferenceObfuscator preferenceObfuscator) {
    this.context = context;
    this.licenseStatusPrefs = preferenceObfuscator;
  }

  public boolean isContribEnabled() {
    return isEnabledFor(LicenceStatus.CONTRIB);
  }

  public boolean isEnabledFor(@NonNull LicenceStatus licenceStatus) {
    if (this.licenceStatus == null) {
      return false;
    }
    return this.licenceStatus.ordinal() >= licenceStatus.ordinal();
  }

  public boolean isUpgradeable() {
    return licenceStatus == null || licenceStatus.isUpgradeable();
  }

  public void init() {
    String licenseStatusPrefsString = licenseStatusPrefs.getString(LICENSE_STATUS_KEY, null);
    try {
      licenceStatus = licenseStatusPrefsString != null ? LicenceStatus.valueOf(licenseStatusPrefsString) : null;
    } catch (IllegalArgumentException e) {
      licenceStatus = null;
    }
  }

  public final void update() {
    Template.updateNewPlanEnabled();
    Account.updateNewAccountEnabled();
    GenericAccountService.updateAccountsIsSyncable(context);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      ShortcutHelper.configureSplitShortcut(context, isContribEnabled());
    }
    AbstractWidget.updateWidgets(context, TemplateWidget.class);
  }

  public void updateLicenceStatus(Licence licence) {
    if (licence == null || licence.getType() == null) {
      licenceStatus = null;
      licenseStatusPrefs.remove(LICENSE_STATUS_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
    } else {
      licenceStatus = licence.getType();
      licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licenceStatus.name());
      if (licence.getValidSince() != null) {
        licenseStatusPrefs.putString(LICENSE_VALID_SINCE_KEY, String.valueOf(licence.getValidSince().getTime()));
      }
      if (licence.getValidUntil() != null) {
        licenseStatusPrefs.putString(LICENSE_VALID_UNTIL_KEY, String.valueOf(licence.getValidUntil().getTime()));
      } else {
        licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
      }
    }
    licenseStatusPrefs.commit();
    update();
  }

  public void reset() {
    init();
    update();
  }

  @VisibleForTesting
  public void setLockState(boolean locked) {
    if (MyApplication.isInstrumentationTest()) {
      licenceStatus = locked ? null : LicenceStatus.CONTRIB;
      update();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public String getFormattedPrice(Package aPackage) {
    return aPackage.getFormattedPrice(context);
  }

  public String getValidUntil() {
    return Utils.getDateFormatSafe(context).format(new Date(Long.parseLong(
        licenseStatusPrefs.getString(LICENSE_VALID_UNTIL_KEY, "0"))));
  }

  public enum LicenceStatus {
    CONTRIB(R.string.contrib_key), EXTENDED(R.string.extended_key), PROFESSIONAL(R.string.professional_key) {
      @Override
      public boolean isUpgradeable() {
        return false;
      }
    };

    private final int resId;

    LicenceStatus(int resId) {
      this.resId = resId;
    }

    public int getResId() {
      return resId;
    }

    public boolean greaterOrEqual(LicenceStatus other) {
      return other == null || compareTo(other) >= 0;
    }

    public boolean covers(ContribFeature contribFeature) {
      if (contribFeature == null) return true;
      return greaterOrEqual(contribFeature.getLicenceStatus());
    }

    public boolean isUpgradeable() {
      return true;
    }
  }
}