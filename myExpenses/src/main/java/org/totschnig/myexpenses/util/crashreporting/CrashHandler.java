package org.totschnig.myexpenses.util.crashreporting;

import android.content.Context;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.licence.LicenceStatus;

import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public abstract class CrashHandler {
  private static final String CUSTOM_DATA_KEY_BREADCRUMB = "Breadcrumb";
  private String currentBreadCrumb;
  private boolean enabled;

  public static void reportWithDbSchema(Exception e) {
    report(e, DbUtils.getSchemaDetails());
  }

  public static void report(Exception e, Map<String, String> customData) {
    for (Map.Entry<String, String> entry : customData.entrySet()) {
      Timber.e("%s: %s", entry.getKey(), entry.getValue());
    }
    Timber.e(e);
  }

  public static void report(Exception e, String key, String data) {
    Timber.e("%s: %s", key, data);
    report(e);
  }

  public static void report(Throwable e) {
    report(e, null);
  }

  public static void report(Throwable e, String tag) {
    if (tag != null) {
      Timber.tag(tag);
    }
    Timber.e(e);
  }

  public static void report(String message) {
    report(new Exception(message));
  }

  public abstract void onAttachBaseContext(MyApplication application);

  public void setupLogging(Context context) {
    enabled = PrefKey.CRASHREPORT_ENABLED.getBoolean(true);
    if (enabled) {
      setupLoggingDo(context);
      putCustomData("Distribution", DistribHelper.getVersionInfo(context));
      putCustomData("Installer", context.getPackageManager().getInstallerPackageName(context.getPackageName()));
      putCustomData("Locale", Locale.getDefault().toString());
    }
  }

  abstract void setupLoggingDo(Context context);

  abstract void putCustomData(String key, String value);

  public synchronized void addBreadcrumb(String breadcrumb) {
    Timber.i("Breadcrumb: %s", breadcrumb);
    if (enabled) {
      currentBreadCrumb = currentBreadCrumb == null ? "" : currentBreadCrumb.substring(Math.max(0, currentBreadCrumb.length() - 500));
      currentBreadCrumb += "->" + breadcrumb;
      putCustomData(CUSTOM_DATA_KEY_BREADCRUMB, currentBreadCrumb);
    }
  }

  public static CrashHandler NO_OP = new CrashHandler() {
    @Override
    public void onAttachBaseContext(MyApplication application) {

    }

    @Override
    void setupLoggingDo(Context context) {

    }

    @Override
    void putCustomData(String key, String value) {

    }
  };

  public void setLicenceStatus(@NonNull LicenceStatus licenceStatus) {
    if (enabled) {
      putCustomData("Licence", licenceStatus.name());
    }
  }
}
