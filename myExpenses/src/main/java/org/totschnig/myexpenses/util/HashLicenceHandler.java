package org.totschnig.myexpenses.util;

import android.provider.Settings;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;

public class HashLicenceHandler extends LicenceHandler {

  public HashLicenceHandler(MyApplication context) {
    super(context);
  }

  @Override
  public void init() {
    super.init();
    if (licenceStatus == null) {
      updateLicenceKeyLegacy();
    }
  }

  @Override
  protected void setLockStateDo(boolean locked) {
    licenceStatus = locked ? null : LicenceStatus.CONTRIB;
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
