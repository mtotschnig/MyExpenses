package org.totschnig.myexpenses;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;
import android.util.Log;

public final class MyTestRunner extends AndroidJUnitRunner {
  public MyTestRunner() {
    // Inform the app we are an instrumentation test before the object graph is initialized.
    Log.d("instrumentationTest", "now setting instrumentationTest to true");
    MyApplication.instrumentationTest = true;
  }

  @Override
  public void finish(int resultCode, Bundle results) {
    MyApplication.cleanUpAfterTest();
    super.finish(resultCode, results);
  }
}
