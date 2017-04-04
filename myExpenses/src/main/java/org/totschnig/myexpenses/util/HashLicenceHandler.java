package org.totschnig.myexpenses.util;

import android.content.Context;
import android.provider.Settings;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;

public class HashLicenceHandler extends LicenceHandler {
  private LicenceStatus contribEnabled = null;

  public HashLicenceHandler(Context context) {
    super(context);
  }

  @Override
  public boolean isContribEnabled() {
    return contribEnabled != null;
  }

  @Override
  public boolean isExtendedEnabled() {
    return contribEnabled == LicenceStatus.EXTENDED;
  }

  @Override
  public void init() {
    updateLicenceKey();
  }

  @Override
  protected void setLockStateDo(boolean locked) {
    contribEnabled = locked ? null : LicenceStatus.CONTRIB;
  }

  public LicenceStatus updateLicenceKey() {
    String key = PrefKey.ENTER_LICENCE.getString("");
    String secret = MyApplication.CONTRIB_SECRET;
    String extendedSecret = secret + "_EXTENDED";
    String androidId = Settings.Secure.getString(MyApplication.getInstance()
        .getContentResolver(), Settings.Secure.ANDROID_ID);
    String s = androidId + extendedSecret;
    Long l = (s.hashCode() & 0x00000000ffffffffL);
    if (l.toString().equals(key)) {
      contribEnabled = LicenceStatus.EXTENDED;
    } else {
      s = androidId + secret;
      l = (s.hashCode() & 0x00000000ffffffffL);
      contribEnabled = l.toString().equals(key) ? LicenceStatus.CONTRIB : null;
    }
    return contribEnabled;
  }
}
