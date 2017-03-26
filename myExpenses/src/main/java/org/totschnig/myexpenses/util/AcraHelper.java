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
  private static final boolean DO_REPORT = Utils.IS_FLAVOURED && !BuildConfig.DEBUG
      && !MyApplication.isInstrumentationTest();

  public static void reportWithDbSchema(Exception e) {
    if (DO_REPORT) {
      report(e, DbUtils.getSchemaDetails());
    } else {
      Timber.e(e, "Report");
    }
  }

  public static void report(Exception e, String key, String data) {
    if (DO_REPORT) {
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
    if (DO_REPORT) {
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
    if (DO_REPORT) {
      ACRA.getErrorReporter().handleSilentException(e);
    } else {
      Timber.e(e);
    }
  }

  public static void report(String message) {
    report(new Exception(message));
  }
}
