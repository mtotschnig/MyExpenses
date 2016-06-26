package org.totschnig.myexpenses.util;

import android.content.Context;

import org.totschnig.myexpenses.BuildConfig;

public interface LicenceHandlerIFace {
  public static boolean HAS_EXTENDED = !BuildConfig.FLAVOR.equals("blackberry");

  void init(Context ctx);

  boolean isContribEnabled();

  boolean isExtendedEnabled();

  void invalidate();

  enum LicenceStatus {
    CONTRIB, EXTENDED
  }
}