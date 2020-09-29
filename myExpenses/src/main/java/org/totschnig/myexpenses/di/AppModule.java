package org.totschnig.myexpenses.di;

import android.content.Context;
import android.os.Bundle;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.PreferencesCurrencyContext;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefHandlerImpl;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.AcraCrashHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.locale.UserLocalProviderImpl;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;
import org.totschnig.myexpenses.util.tracking.Tracker;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

import static org.totschnig.myexpenses.di.AppComponent.USER_COUNTRY;

@Module
public class AppModule {

  @Provides
  @Singleton
  static Context provideContext(MyApplication myApplication) {
    return myApplication;
  }

  @Provides
  @Singleton
  static CrashHandler providesCrashHandler() {
    return (MyApplication.isInstrumentationTest()) ? CrashHandler.NO_OP : new AcraCrashHandler();
  }

  @Provides
  @Singleton
  static Tracker provideTracker() {
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
  @Named(USER_COUNTRY)
  static String provideUserCountry() {
    final String defaultCountry = "us";
    if (BuildConfig.DEBUG) {
      return defaultCountry;
    } else {
      final String countryFromTelephonyManager = Utils.getCountryFromTelephonyManager();
      return countryFromTelephonyManager != null ? countryFromTelephonyManager : defaultCountry;
    }
  }

  @Provides
  @Singleton
  static PrefHandler providePrefHandler(MyApplication context) {
    return new PrefHandlerImpl(context);
  }

  @Provides
  @Singleton
  static CurrencyContext provideCurrencyContext(PrefHandler prefHandler) {
    return new PreferencesCurrencyContext(prefHandler);
  }

  @Provides
  @Singleton
  static UserLocaleProvider provideUserLocaleProvider(PrefHandler prefHandler, Locale locale) {
    return new UserLocalProviderImpl(prefHandler, locale);
  }
}
