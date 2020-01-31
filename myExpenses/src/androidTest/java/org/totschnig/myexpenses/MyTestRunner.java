package org.totschnig.myexpenses;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import org.totschnig.myexpenses.util.Utils;

import androidx.test.runner.AndroidJUnitRunner;

public final class MyTestRunner extends AndroidJUnitRunner {
  private boolean ANIMATION_SETTINGS_MANUALLY_CHECKED = false;
  public MyTestRunner() {
    // Inform the app we are an instrumentation test before the object graph is initialized.
    Log.d("instrumentationTest", "now setting instrumentationTest to true");
    MyApplication.setInstrumentationTest(true);
  }

  @Override
  public Application newApplication(ClassLoader cl, String className, Context context) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    return super.newApplication(cl, TestApp.class.getName(), context);
  }

  @Override
  @SuppressLint("NewApi")
  public void onStart() {
    if (!ANIMATION_SETTINGS_MANUALLY_CHECKED) {
      boolean isJellyBean = Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN);
      boolean isJellyBeanMr1 = Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN_MR1);
      String[] criticalSettings = new String[isJellyBean ? 3 : 2];
      criticalSettings[0] = isJellyBeanMr1 ? Settings.Global.TRANSITION_ANIMATION_SCALE :
          Settings.System.TRANSITION_ANIMATION_SCALE;
      criticalSettings[1] = isJellyBeanMr1 ? Settings.Global.WINDOW_ANIMATION_SCALE :
          Settings.System.WINDOW_ANIMATION_SCALE;
      if (isJellyBean) {
        //noinspection InlinedApi
        criticalSettings[2] = isJellyBeanMr1 ? Settings.Global.ANIMATOR_DURATION_SCALE :
            Settings.System.ANIMATOR_DURATION_SCALE;
      }

      for (String setting : criticalSettings) {
        if (isJellyBeanMr1) {
          try {
            checkSettingGlobal(setting);
          } catch (Settings.SettingNotFoundException e) {
            checkSettingSystem(setting);
          }
        } else {
          checkSettingSystem(setting);
        }
      }
    }
    super.onStart();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private void checkSettingGlobal(String setting) throws Settings.SettingNotFoundException {
    if (Settings.Global.getFloat(getTargetContext().getContentResolver(), setting) != 0) {
      throw new AnimationsNotDisabledException(setting);
    }
  }

  private void checkSettingSystem(String setting) {
    try {
      if (Settings.System.getFloat(getTargetContext().getContentResolver(), setting) != 0) {
        throw new AnimationsNotDisabledException(setting);
      }
    } catch (Settings.SettingNotFoundException e) {
      throw new AnimationsNotDisabledException(setting, "%s  setting not found");
    }
  }

  @Override
  public void finish(int resultCode, Bundle results) {
    MyApplication.cleanUpAfterTest();
    super.finish(resultCode, results);
  }

  public static class AnimationsNotDisabledException extends RuntimeException {
    String setting;
    String messageFormat;

    public AnimationsNotDisabledException(String setting) {
      this(setting, "%s  must be disabled for reliable Espresso tests");
    }

    public AnimationsNotDisabledException(String setting, String messageFormat) {
      this.setting = setting;
      this.messageFormat = messageFormat;
    }

    @Override
    public String getMessage() {
      return String.format(messageFormat, setting);
    }
  }
}
