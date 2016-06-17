package org.totschnig.myexpenses;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.test.runner.AndroidJUnitRunner;
import android.util.Log;

import org.totschnig.myexpenses.util.Utils;

public final class MyTestRunner extends AndroidJUnitRunner {
  public MyTestRunner() {
    // Inform the app we are an instrumentation test before the object graph is initialized.
    Log.d("instrumentationTest", "now setting instrumentationTest to true");
    MyApplication.setInstrumentationTest(true);
  }

  @Override
  public void onStart() {
    boolean isJellyBean = Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN);
    String[] criticalSettings = new String[isJellyBean ? 3 : 2];
    criticalSettings[0] = Settings.System.TRANSITION_ANIMATION_SCALE;
    criticalSettings[1] = Settings.System.WINDOW_ANIMATION_SCALE;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      //noinspection InlinedApi
      criticalSettings[2] = Settings.System.ANIMATOR_DURATION_SCALE;
    }

    try {
      for (String setting : criticalSettings) {
        if (Settings.System.getFloat(getContext().getContentResolver(), setting) != 0) {
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

  public static class AnimationsNotDisabledException extends RuntimeException {
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
