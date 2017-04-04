package org.totschnig.myexpenses.util;

import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

public abstract class LicenceHandler {
  public static boolean HAS_EXTENDED = !DistribHelper.isBlackberry();
  protected final Context context;

  protected LicenceHandler(Context context) {
    this.context = context;
  }

  public abstract boolean isContribEnabled();

  public abstract boolean isExtendedEnabled();

  public boolean isNoLongerUpgradeable() {
    return isExtendedEnabled() || (isContribEnabled() && !HAS_EXTENDED);
  }

  public abstract void init();

  public final void update() {
    Template.updateNewPlanEnabled();
    Account.updateNewAccountEnabled();
    GenericAccountService.updateAccountsIsSyncable(context);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      ShortcutHelper.configureSplitShortcut(context, isContribEnabled());
    }
    AbstractWidget.updateWidgets(context, TemplateWidget.class);
  }

  public void reset() {
    init();
    update();
  }

  @VisibleForTesting
  public void setLockState(boolean locked) {
    if (MyApplication.isInstrumentationTest()) {
      setLockStateDo(locked);
      update();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  protected abstract void setLockStateDo(boolean locked);


  public enum LicenceStatus {
    CONTRIB, EXTENDED
  }
}