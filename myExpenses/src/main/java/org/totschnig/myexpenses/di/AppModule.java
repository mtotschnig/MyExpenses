package org.totschnig.myexpenses.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.PreferencesCurrencyContext;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefHandlerImpl;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandlerImpl;
import org.totschnig.myexpenses.util.licence.HashLicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

@Module
public class AppModule {
  protected MyApplication application;

  public AppModule(MyApplication application) {
    this.application = application;
  }

  @Provides
  @Singleton
  MyApplication provideApplication() {
    return application;
  }

  @Provides
  @Singleton
  LicenceHandler providesLicenceHandler(PreferenceObfuscator preferenceObfuscator, CrashHandler crashHandler) {
    return new HashLicenceHandler(application, preferenceObfuscator, crashHandler);
  }

  @Provides
  @Singleton
  CrashHandler providesCrashHandler() {
    return (MyApplication.isInstrumentationTest()) ? CrashHandler.NO_OP : new CrashHandlerImpl();
  }

  @Provides
  @Singleton
  Tracker provideTracker() {
    try {
      return (Tracker) Class.forName(
          "org.totschnig.myexpenses.util.tracking.PlatformTracker").newInstance();
    } catch (Exception e) {
      return new Tracker() {
        @Override
        public void init(Context context) {
          //noop
        }

        @Override
        public void logEvent(String eventName, Bundle params) {
          Timber.d("Event %s (%s)", eventName, params);
        }

        @Override
        public void setEnabled(boolean enabled) {
          //noop
        }
      };
    }
  }

  @Provides
  @Singleton
  @Named("deviceId")
  protected String provideDeviceId() {
    return Settings.Secure.getString(application.getContentResolver(), Settings.Secure.ANDROID_ID);
  }

  @Provides
  @Singleton
  @Named("userCountry")
  protected String provideUserCountry() {
    return BuildConfig.DEBUG ? "de" : Utils.getCountryFromTelephonyManager();
  }

  @Provides
  @Singleton
  PreferenceObfuscator provideLicencePrefs(Obfuscator obfuscator) {
    String PREFS_FILE = "license_status_new";
    SharedPreferences sp = application.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    return new PreferenceObfuscator(sp, obfuscator);
  }

  @Provides
  @Singleton
  protected Obfuscator provideObfuscator(@Named("deviceId") String deviceId) {
    byte[] SALT = new byte[]{
        -1, -124, -4, -59, -52, 1, -97, -32, 38, 59, 64, 13, 45, -104, -3, -92, -56, -49, 65, -25
    };
    return new AESObfuscator(SALT, application.getPackageName(), deviceId);
  }

  @Provides
  @Singleton
  protected PrefHandler providePrefHandler(MyApplication context) {
    return new PrefHandlerImpl(context);
  }


  @Provides
  @Singleton
  protected CurrencyContext provideCurrencyContext(PrefHandler prefHandler) {
    return new PreferencesCurrencyContext(prefHandler);
  }
}
