package org.totschnig.myexpenses.util;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DbUtils;

import java.util.Map;

import timber.log.Timber;

//TODO move to Timber
public class AcraHelper {
  public static final boolean DO_REPORT = !DistribHelper.isGithub() && !BuildConfig.DEBUG
      && !MyApplication.isInstrumentationTest();

  public static void reportWithDbSchema(Exception e) {
    if (shouldReport()) {
      report(e, DbUtils.getSchemaDetails());
    } else {
      Timber.e(e, "Report");
    }
  }

  public static void report(Exception e, String key, String data) {
    if (shouldReport()) {
      ErrorReporter errorReporter = ACRA.getErrorReporter();
      errorReporter.putCustomData(key, data);
      errorReporter.handleSilentException(e);
      errorReporter.removeCustomData(key);
    } else {
      Timber.e("%s: %s", key, data);
      report(e);
    }
  }

  public static void report(Exception e, Map<String, String> customData) {
    if (shouldReport()) {
      ErrorReporter errorReporter = ACRA.getErrorReporter();
      for (Map.Entry<String, String> entry : customData.entrySet()) {
        errorReporter.putCustomData(entry.getKey(), entry.getValue());
      }
      errorReporter.handleSilentException(e);
      for (String key : customData.keySet()) {
        errorReporter.removeCustomData(key);
      }
    } else {
      for (Map.Entry<String, String> entry : customData.entrySet()) {
        Timber.e("%s: %s", entry.getKey(), entry.getValue());
      }
      Timber.e(e);
    }
  }

  public static void report(Exception e) {
    if (shouldReport()) {
      ACRA.getErrorReporter().handleSilentException(e);
    } else {
      Timber.e(e);
    }
  }

  public static void report(String message) {
    report(new Exception(message));
  }

  private static boolean shouldReport() {
    return DO_REPORT && ACRA.isInitialised();
  }

  public static void appendCustomData(String key, String value) {
    String currentValue = ACRA.getErrorReporter().getCustomData(key);
    String trimmedValue = currentValue == null ? "" : currentValue.substring(Math.max(0, currentValue.length() - 500));
    ACRA.getErrorReporter().putCustomData(key, trimmedValue + "->" + value);
  }
}
