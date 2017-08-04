package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.model.ContribFeature;

public abstract class AdHandler {
  protected static final int DAY_IN_MILLIS = BuildConfig.DEBUG ? 1 : 86400000;
  private static final int INITIAL_GRACE_DAYS = BuildConfig.DEBUG ? 0 : 5;
  protected final ViewGroup adContainer;
  protected Context context;

  protected AdHandler(ViewGroup adContainer) {
    this.adContainer = adContainer;
    this.context = adContainer.getContext();
  }

  public abstract void init();

  protected boolean isAdDisabled() {
    return isAdDisabled(context);
  }

  public static boolean isAdDisabled(Context context) {
    return !BuildConfig.DEBUG &&
        (ContribFeature.AD_FREE.hasAccess() ||
            isInInitialGracePeriod(context));
  }

  private static boolean isInInitialGracePeriod(Context context) {
    try {
      return System.currentTimeMillis() -
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
              .firstInstallTime < DAY_IN_MILLIS * INITIAL_GRACE_DAYS;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  public void onEditTransactionResult() {

  }

  public void onResume() {

  }

  public void onDestroy() {

  }

  public void onPause() {

  }
  protected final void hide() {
    adContainer.setVisibility(View.GONE);
  }
}
