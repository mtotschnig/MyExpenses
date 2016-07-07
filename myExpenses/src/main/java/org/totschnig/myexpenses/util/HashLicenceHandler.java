package org.totschnig.myexpenses.util;

import android.content.Context;
import android.provider.Settings;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;

public class HashLicenceHandler extends LicenceHandler {
  private LicenceStatus contribEnabled = null;
  private boolean contribEnabledInitialized = false;

  @Override
  public void init(Context ctx) {
  }

  @Override
  public boolean isContribEnabled() {
    if (!contribEnabledInitialized) {
      contribEnabled = verifyLicenceKey();
      contribEnabledInitialized = true;
    }
    return contribEnabled!=null;
  }

  @Override
  public boolean isExtendedEnabled() {
    if (!contribEnabledInitialized) {
      contribEnabled = verifyLicenceKey();
      contribEnabledInitialized = true;
    }
    return contribEnabled == LicenceStatus.EXTENDED;
  }

  @Override
  public void invalidate() {
    super.invalidate();
    this.contribEnabledInitialized = false;
  }

  public LicenceStatus verifyLicenceKey() {
    String key = PrefKey.ENTER_LICENCE.getString("");
    String secret= MyApplication.CONTRIB_SECRET;
    String extendedSecret = secret+"_EXTENDED";
    String androidId = Settings.Secure.getString(MyApplication.getInstance()
        .getContentResolver(), Settings.Secure.ANDROID_ID);
    String s = androidId + extendedSecret;
    Long l = (s.hashCode() & 0x00000000ffffffffL);
    if (l.toString().equals(key)) {
      return LicenceStatus.EXTENDED;
    }
    s = androidId + secret;
    l = (s.hashCode() & 0x00000000ffffffffL);
    return l.toString().equals(key) ? LicenceStatus.CONTRIB : null;
  }
}
