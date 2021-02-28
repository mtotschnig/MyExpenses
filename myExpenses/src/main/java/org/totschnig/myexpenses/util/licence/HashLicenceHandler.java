package org.totschnig.myexpenses.util.licence;

import android.provider.Settings;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import androidx.annotation.Nullable;

@Deprecated
public class HashLicenceHandler extends LicenceHandler {
  private boolean hasLegacyLicence = false;

  public HashLicenceHandler(MyApplication context, PreferenceObfuscator preferenceObfuscator, CrashHandler crashHandler, PrefHandler prefHandler) {
    super(context, preferenceObfuscator, crashHandler, prefHandler);
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
    if (getLicenceStatus() == null) {
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
        maybeUpgradeLicence(LicenceStatus.EXTENDED);
        hasLegacyLicence = true;
      } else {
        s = androidId + secret;
        l = (s.hashCode() & 0x00000000ffffffffL);
        if (l.toString().equals(key)) {
          maybeUpgradeLicence(LicenceStatus.CONTRIB);
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
