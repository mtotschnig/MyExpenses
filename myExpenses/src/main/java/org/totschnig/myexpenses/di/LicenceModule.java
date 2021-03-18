package org.totschnig.myexpenses.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LicenceModule {
  @Provides
  @Singleton
  LicenceHandler providesLicenceHandler(PreferenceObfuscator preferenceObfuscator, CrashHandler crashHandler, MyApplication application, PrefHandler prefHandler) {
    return new LicenceHandler(application, preferenceObfuscator, crashHandler, prefHandler);
  }

  @Provides
  @Singleton
  @Named("deviceId")
  protected String provideDeviceId(MyApplication application) {
    return Settings.Secure.getString(application.getContentResolver(), Settings.Secure.ANDROID_ID);
  }

  @Provides
  @Singleton
  PreferenceObfuscator provideLicencePrefs(Obfuscator obfuscator, MyApplication application) {
    String PREFS_FILE = "license_status_new";
    SharedPreferences sp = application.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    return new PreferenceObfuscator(sp, obfuscator);
  }

  @Provides
  @Singleton
  protected Obfuscator provideObfuscator(@Named("deviceId") String deviceId, MyApplication application) {
    byte[] SALT = new byte[]{
        -1, -124, -4, -59, -52, 1, -97, -32, 38, 59, 64, 13, 45, -104, -3, -92, -56, -49, 65, -25
    };
    return new AESObfuscator(SALT, application.getPackageName(), deviceId);
  }
}
