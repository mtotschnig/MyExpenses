package org.totschnig.myexpenses.util;

import android.util.Log;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DbUtils;

import java.util.Map;

public class AcraHelper {
  private static final boolean DO_REPORT = Utils.IS_FLAVOURED && !BuildConfig.DEBUG
      && !MyApplication.isInstrumentationTest();

  public static void reportWithDbSchema(Exception e) {
    if (DO_REPORT) {
      report(e, DbUtils.getSchemaDetails());
    } else {
      Log.e(MyApplication.TAG, "Report", e);
    }
  }

  public static void report(Exception e, String key, String data) {
    if (DO_REPORT) {
      ErrorReporter errorReporter = ACRA.getErrorReporter();
      errorReporter.putCustomData(key, data);
      errorReporter.handleSilentException(e);
      errorReporter.removeCustomData(key);
    } else {
      Log.e(MyApplication.TAG, key + ": " + data);
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
        Log.e(MyApplication.TAG, entry.getKey() + ": " + entry.getValue());
      }
      Log.e(MyApplication.TAG, "Report", e);
    }
  }

  public static void report(Exception e) {
    if (DO_REPORT) {
      ACRA.getErrorReporter().handleSilentException(e);
    } else {
      Log.e(MyApplication.TAG, "Report", e);
    }
  }

  public static void report(String message) {
    report(new Exception(message));
  }
}
