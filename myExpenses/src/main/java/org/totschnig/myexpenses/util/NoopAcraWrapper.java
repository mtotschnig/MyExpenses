package org.totschnig.myexpenses.util;

import android.app.Application;
import android.util.Log;

import org.totschnig.myexpenses.MyApplication;

public class NoopAcraWrapper implements AcraWrapperIFace {

  @Override
  public void init(Application context) {}

  @Override
  public boolean isACRASenderServiceProcess() {
    return false;
  }

  @Override
  public void reportToAcraWithDbSchema(Exception e) {
    //reportToAcra(e, "DB_SCHEMA", DbUtils.getTableDetails());
    reportToAcra(e);
  }

  @Override
  public void reportToAcra(Exception e, String key, String data) {
    // ErrorReporter errorReporter = org.acra.ACRA.getErrorReporter();
    // errorReporter.putCustomData(key, data);
    // errorReporter.handleException(e);
    // errorReporter.removeCustomData(key);
    Log.e(MyApplication.TAG, key + ": " + data);
    reportToAcra(e);
  }

  @Override
  public void reportToAcra(Exception e) {
    Log.e(MyApplication.TAG, "Report", e);
    /* org.acra.ACRA.getErrorReporter().handleSilentException(e); */
  }
}
