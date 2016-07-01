package org.totschnig.myexpenses.util;

import android.util.Log;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DbUtils;

public class AcraHelper {
  private static final boolean DO_REPORT = Utils.IS_FLAVOURED && !BuildConfig.DEBUG;

  public static void reportWithDbSchema(Exception e) {
    if (DO_REPORT) {
      reportWithTableDetails(e, DbUtils.getTableDetails());
    } else {
      Log.e(MyApplication.TAG, "Report", e);
    }
  }

  public static void reportWithTableDetails(Exception e, String[][] schema) {
    if (DO_REPORT) {
      ErrorReporter errorReporter = ACRA.getErrorReporter();
      for (String[] tableInfo : schema) {
        errorReporter.putCustomData(tableInfo[0], tableInfo[1]);
      }
      errorReporter.handleSilentException(e);
      for (String[] tableInfo : schema) {
        errorReporter.removeCustomData(tableInfo[0]);
      }
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

  public static void report(Exception e) {
    if (DO_REPORT) {
      ACRA.getErrorReporter().handleSilentException(e);
    } else {
      Log.e(MyApplication.TAG, "Report", e);
    }
  }
}
