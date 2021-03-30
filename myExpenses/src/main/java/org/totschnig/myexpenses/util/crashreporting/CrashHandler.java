package org.totschnig.myexpenses.util.crashreporting;

import android.content.Context;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
      Timber.w("%s: %s", entry.getKey(), entry.getValue());
    }
    report(e);
  }

  public static void report(Exception e, String key, String data) {
    Timber.w("%s: %s", key, data);
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

  public static void reportWithTag(String message, String tag) {
    final Exception e = new Exception(message);
    e.fillInStackTrace();
    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
    stack.remove(0);
    e.setStackTrace(stack.toArray(new StackTraceElement[0]));
    report(e, tag);
  }

  public static void reportWithFormat(String format, Object... args) {
    final Exception e = new Exception(String.format(format, args));
    e.fillInStackTrace();
    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
    stack.remove(0);
    e.setStackTrace(stack.toArray(new StackTraceElement[0]));
    report(e);
  }

  public static void report(String message) {
    final Exception e = new Exception(message);
    e.fillInStackTrace();
    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
    stack.remove(0);
    e.setStackTrace(stack.toArray(new StackTraceElement[0]));
    report(e);
  }

  public abstract void onAttachBaseContext(MyApplication application);

  public void setupLogging(Context context) {
    enabled = PrefKey.CRASHREPORT_ENABLED.getBoolean(true);
    if (enabled) {
      setupLoggingDo(context);
    }
  }

  protected void setKeys(Context context) {
    putCustomData("Distribution", DistributionHelper.getVersionInfo(context));
    putCustomData("Installer", context.getPackageManager().getInstallerPackageName(context.getPackageName()));
    putCustomData("Locale", Locale.getDefault().toString());
  }

  abstract void setupLoggingDo(Context context);

  public void setUserEmail(String value) {
    putCustomData("UserEmail", value);
  }

  public abstract void putCustomData(@NonNull String key, @Nullable String value);

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
    public void putCustomData(@NonNull String key, String value) {

    }
  };

  public void initProcess(Context context, boolean syncService) {}
}
