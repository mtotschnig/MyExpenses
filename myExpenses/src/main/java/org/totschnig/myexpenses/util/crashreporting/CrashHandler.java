package org.totschnig.myexpenses.util.crashreporting;

import android.content.Context;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DbUtils;

import java.util.Map;

import timber.log.Timber;

public abstract class CrashHandler {
  private static final String CUSTOM_DATA_KEY_BREADCRUMB = "Breadcrumb";
  private String currentBreadCrumb;

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
    Timber.e(e);
  }

  public static void report(String message) {
    report(new Exception(message));
  }

  public abstract void onAttachBaseContext(MyApplication application);
  public abstract void setupLogging(Context context);
  public abstract void putCustomData(String key, String value);
  public void addBreadcrumb(String breadcrumb) {
    currentBreadCrumb = currentBreadCrumb == null ? "" : currentBreadCrumb.substring(Math.max(0, currentBreadCrumb.length() - 500));
    currentBreadCrumb += "->" + breadcrumb;
    putCustomData(CUSTOM_DATA_KEY_BREADCRUMB, currentBreadCrumb);
  }

  public static CrashHandler NO_OP = new CrashHandler() {
    @Override
    public void onAttachBaseContext(MyApplication application) {

    }

    @Override
    public void setupLogging(Context context) {

    }

    @Override
    public void putCustomData(String key, String value) {

    }
  };
}
