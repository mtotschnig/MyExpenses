package org.totschnig.myexpenses;

import android.annotation.SuppressLint;
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
  @SuppressLint("NewApi")
  public void onStart() {
    boolean isJellyBean = Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN);
    String[] criticalSettings = new String[isJellyBean ? 3 : 2];
    criticalSettings[0] = isJellyBean ? Settings.Global.TRANSITION_ANIMATION_SCALE :
        Settings.System.TRANSITION_ANIMATION_SCALE;
    criticalSettings[1] = isJellyBean ? Settings.Global.WINDOW_ANIMATION_SCALE :
        Settings.System.WINDOW_ANIMATION_SCALE;
    if (isJellyBean) {
      //noinspection InlinedApi
      criticalSettings[2] = Settings.Global.ANIMATOR_DURATION_SCALE;
    }

    for (String setting : criticalSettings) {
      try {
         float aSetting = isJellyBean ?
            Settings.Global.getFloat(getContext().getContentResolver(), setting) :
            Settings.System.getFloat(getContext().getContentResolver(), setting);
        if (aSetting != 0) {
          throw new AnimationsNotDisabledException(setting);
        }
      } catch (Settings.SettingNotFoundException e) {
        throw new RuntimeException(String.format("Unable to determine animation settings for %s", setting));
      }
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
