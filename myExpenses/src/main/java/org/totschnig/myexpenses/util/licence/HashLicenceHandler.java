package org.totschnig.myexpenses.util.licence;

import android.provider.Settings;
import android.support.annotation.Nullable;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;

@Deprecated
public class HashLicenceHandler extends LicenceHandler {
  private boolean hasLegacyLicence = false;

  public HashLicenceHandler(MyApplication context, PreferenceObfuscator preferenceObfuscator) {
    super(context, preferenceObfuscator);
  }

  @Override
  public boolean hasLegacyLicence() {
    return hasLegacyLicence;
  }

  @Override
  public boolean needsMigration() {
    return hasLegacyLicence;
  }

  @Override
  public void init() {
    super.init();
    if (licenceStatus == null) {
      updateLicenceKeyLegacy();
    }
  }

  @Override
  public void updateLicenceStatus(Licence licence) {
    if (hasLegacyLicence && licence != null && licence.getType() != null) {
      PrefKey.LICENCE_LEGACY.remove();
      hasLegacyLicence = false;
    }
    super.updateLicenceStatus(licence);
  }

  private void updateLicenceKeyLegacy() {
    String key = PrefKey.LICENCE_LEGACY.getString("");
    if (!"".equals(key)) {
      String secret = MyApplication.CONTRIB_SECRET;
      String extendedSecret = secret + "_EXTENDED";
      String androidId = Settings.Secure.getString(MyApplication.getInstance()
          .getContentResolver(), Settings.Secure.ANDROID_ID);
      String s = androidId + extendedSecret;
      Long l = (s.hashCode() & 0x00000000ffffffffL);
      if (l.toString().equals(key)) {
        licenceStatus = LicenceStatus.EXTENDED;
        hasLegacyLicence = true;
      } else {
        s = androidId + secret;
        l = (s.hashCode() & 0x00000000ffffffffL);
        if (l.toString().equals(key)) {
          licenceStatus = LicenceStatus.CONTRIB;
          hasLegacyLicence = true;
        }
      }
    }
  }

  @Nullable
  @Override
  public String getExtendedUpgradeGoodieMessage(Package selectedPackage) {
    return hasLegacyLicence ? null : super.getExtendedUpgradeGoodieMessage(selectedPackage);
  }
}
