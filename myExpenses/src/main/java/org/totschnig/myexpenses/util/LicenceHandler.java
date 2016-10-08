package org.totschnig.myexpenses.util;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Template;

public abstract class LicenceHandler {
  public static boolean HAS_EXTENDED = !BuildConfig.FLAVOR.equals("blackberry");

  public abstract void init(Context ctx);

  public abstract boolean isContribEnabled();

  public abstract boolean isExtendedEnabled();

  public void invalidate() {
    Template.updateNewPlanEnabled();
  }

  @VisibleForTesting
  public void setLockState(boolean locked) {
    if (MyApplication.isInstrumentationTest()) {
      setLockStateDo(locked);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  protected abstract void setLockStateDo(boolean locked);
  
  public enum LicenceStatus {
    CONTRIB, EXTENDED
  }
}