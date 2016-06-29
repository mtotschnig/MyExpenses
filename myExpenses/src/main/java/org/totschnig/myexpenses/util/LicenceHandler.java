package org.totschnig.myexpenses.util;

import android.content.Context;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.model.Template;

public abstract class LicenceHandler {
  public static boolean HAS_EXTENDED = !BuildConfig.FLAVOR.equals("blackberry");

  public abstract void init(Context ctx);

  public abstract boolean isContribEnabled();

  public abstract boolean isExtendedEnabled();

  public void invalidate() {
    Template.updateNewPlanEnabled();
  }

  public enum LicenceStatus {
    CONTRIB, EXTENDED
  }
}