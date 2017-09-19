package org.totschnig.myexpenses.util;

import android.os.Build;
import android.support.annotation.VisibleForTesting;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.util.licence.Licence;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

import javax.inject.Inject;

public abstract class LicenceHandler {
  private static final String LICENSE_STATUS_KEY = "licence_status";
  private static final String LICENSE_VALID_SINCE_KEY = "licence_valid_since";
  private static final String LICENSE_VALID_UNTIL_KEY = "licence_valid_until";
  public static boolean HAS_EXTENDED = !DistribHelper.isBlackberry();
  protected final MyApplication context;
  protected LicenceStatus licenceStatus;
  @Inject
  PreferenceObfuscator licenseStatusPrefs;

  protected LicenceHandler(MyApplication context) {
    this.context = context;
    context.getAppComponent().inject(this);
  }

  public boolean isContribEnabled() {
    return licenceStatus != null;
  }

  public boolean isExtendedEnabled() {
    return licenceStatus == LicenceStatus.EXTENDED;
  }

  public boolean isNoLongerUpgradeable() {
    return isExtendedEnabled() || (isContribEnabled() && !HAS_EXTENDED);
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
    if (licence == null) {
      licenceStatus = null;
      licenseStatusPrefs.remove(LICENSE_STATUS_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_SINCE_KEY);
      licenseStatusPrefs.remove(LICENSE_VALID_UNTIL_KEY);
    } else {
      licenceStatus = licence.getType();
      licenseStatusPrefs.putString(LICENSE_STATUS_KEY, licenceStatus.name());
      licenseStatusPrefs.putString(LICENSE_VALID_SINCE_KEY, String.valueOf(licence.getValidSince().getTime()));
      licenseStatusPrefs.putString(LICENSE_VALID_UNTIL_KEY, String.valueOf(licence.getValidUntil().getTime()));
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
      setLockStateDo(locked);
      update();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  protected abstract void setLockStateDo(boolean locked);


  public enum LicenceStatus {
    CONTRIB(R.string.contrib_key), EXTENDED(R.string.extended_key);

    private final int resId;

    LicenceStatus(int resId) {
      this.resId = resId;
    }

    public int getResId() {
      return resId;
    }
  }
}