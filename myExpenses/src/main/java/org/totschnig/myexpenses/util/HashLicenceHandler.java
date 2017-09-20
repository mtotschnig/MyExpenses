package org.totschnig.myexpenses.util;

import android.provider.Settings;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;

@Deprecated
public class HashLicenceHandler extends LicenceHandler {

  public HashLicenceHandler(MyApplication context, PreferenceObfuscator preferenceObfuscator) {
    super(context, preferenceObfuscator);
  }

  @Override
  public void init() {
    super.init();
    if (licenceStatus == null) {
      updateLicenceKeyLegacy();
    }
  }

  private void updateLicenceKeyLegacy() {
    String key = PrefKey.ENTER_LICENCE.getString("");
    if (!"".equals(key)) {
      String secret = MyApplication.CONTRIB_SECRET;
      String extendedSecret = secret + "_EXTENDED";
      String androidId = Settings.Secure.getString(MyApplication.getInstance()
          .getContentResolver(), Settings.Secure.ANDROID_ID);
      String s = androidId + extendedSecret;
      Long l = (s.hashCode() & 0x00000000ffffffffL);
      if (l.toString().equals(key)) {
        licenceStatus = LicenceStatus.EXTENDED;
      } else {
        s = androidId + secret;
        l = (s.hashCode() & 0x00000000ffffffffL);
        licenceStatus = l.toString().equals(key) ? LicenceStatus.CONTRIB : null;
      }
    }
  }
}
