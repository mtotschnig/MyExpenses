package org.totschnig.myexpenses;

import android.os.Bundle;
import android.provider.Settings;
import android.support.test.runner.AndroidJUnitRunner;
import android.util.Log;

public final class MyTestRunner extends AndroidJUnitRunner {
  public MyTestRunner() {
    // Inform the app we are an instrumentation test before the object graph is initialized.
    Log.d("instrumentationTest", "now setting instrumentationTest to true");
    MyApplication.instrumentationTest = true;
  }

  @Override
  public void onStart() {
    String[] criticalSettings = {
        Settings.System.TRANSITION_ANIMATION_SCALE,
        Settings.System.WINDOW_ANIMATION_SCALE,
        Settings.System.ANIMATOR_DURATION_SCALE
    };
    try {
      for (String setting: criticalSettings) {
        if (Settings.Global.getFloat(getContext().getContentResolver(), setting) != 0) {
          throw new AnimationsNotDisabledException(setting);
        }
      }
    } catch (Settings.SettingNotFoundException e) {
      throw new RuntimeException("Unable to determine animation settings");
    }
    super.onStart();
  }

  @Override
  public void finish(int resultCode, Bundle results) {
    MyApplication.cleanUpAfterTest();
    super.finish(resultCode, results);
  }

  public class AnimationsNotDisabledException extends RuntimeException {
    String setting;

    public AnimationsNotDisabledException(String setting) {
      this.setting = setting;
    }

    @Override
    public String getMessage() {
      return String.format("%s  must be disabled for reliable Espresso tests", setting);
    }
  }
}
