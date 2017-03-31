package org.totschnig.myexpenses.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.GenericAccountService;

import java.util.Collections;

public abstract class LicenceHandler {
  public static boolean HAS_EXTENDED = !DistribHelper.isBlackberry();
  protected final Context context;

  protected LicenceHandler(Context context) {
    this.context = context;
  }


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
    GenericAccountService.updateAccountsIsSyncable(context);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      configureSplitShortcut();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  private void configureSplitShortcut() {
    ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

    Intent intent = null;
    if (isContribEnabled()) {
      intent = ShortcutHelper.createIntentForNewSplit(context);
    } else {
      intent = ContribInfoDialogActivity.getIntentFor(context, ContribFeature.SPLIT_TRANSACTION);
    }
    ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "split")
        .setShortLabel(context.getString(R.string.menu_create_split))
        .setIcon(Icon.createWithResource(context, R.drawable.ic_menu_split_shortcut))
        .setIntent(intent)
        .build();
    shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
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