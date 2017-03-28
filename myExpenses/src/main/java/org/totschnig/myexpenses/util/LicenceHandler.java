package org.totschnig.myexpenses.util;

import android.support.annotation.VisibleForTesting;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.GenericAccountService;

public abstract class LicenceHandler {
  public static boolean HAS_EXTENDED = !DistribHelper.isBlackberry();

  public void init() {
    if (PrefKey.CURRENT_VERSION.getInt(-1) != -1) {
      refresh(true);
    }
  }

  public abstract boolean isContribEnabled();

  public abstract boolean isExtendedEnabled();

  public boolean isNoLongerUpgradeable() {
    return isExtendedEnabled() || (isContribEnabled() && !HAS_EXTENDED);
  }

  public final void refresh(boolean invalidate) {
    refreshDo();
    if (invalidate) {
      invalidate();
    }
  }

  protected abstract void refreshDo();

  final void invalidate() {
    Template.updateNewPlanEnabled();
    Account.updateNewAccountEnabled();
    //TODO store context as field in LicenceHandler
    GenericAccountService.updateAccountsIsSyncable(MyApplication.getInstance());
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