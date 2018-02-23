package org.totschnig.myexpenses.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

import org.acra.ReportField;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.CoreConfiguration;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.AcraHelper;
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
  LicenceHandler providesLicenceHandler(PreferenceObfuscator preferenceObfuscator) {
    return new HashLicenceHandler(application, preferenceObfuscator);
  }

  @Provides
  @Singleton
  @Nullable
  CoreConfiguration providesAcraConfiguration() {
    if (MyApplication.isInstrumentationTest()) return null;
    try {
      CoreConfigurationBuilder configurationBuilder = new CoreConfigurationBuilder(application)
          .setEnabled(true)
          .setBuildConfigClass(BuildConfig.class);
      if (AcraHelper.DO_REPORT) {
        configurationBuilder
            .setLogcatArguments("-t", "250", "-v", "long", "ActivityManager:I", "MyExpenses:V", "*:S")
            .setExcludeMatchingSharedPreferencesKeys("planner_calendar_path", "password")
            .getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
            .setEnabled(true)
            .setUri(BuildConfig.ACRA_FORM_URI)
            .setHttpMethod(HttpSender.Method.PUT)
            .setBasicAuthLogin(BuildConfig.ACRA_FORM_URI_BASIC_AUTH_LOGIN)
            .setBasicAuthPassword(BuildConfig.ACRA_FORM_URI_BASIC_AUTH_PASSWORD);
      } else {
        configurationBuilder
            .setReportFormat(StringFormat.KEY_VALUE_LIST)
            .setReportField(ReportField.APP_VERSION_CODE, true)
            .setReportField(ReportField.USER_CRASH_DATE, true)
            .getPluginConfigurationBuilder(DialogConfigurationBuilder.class)
            .setEnabled(true)
            .setResText(R.string.crash_dialog_text)
            .setResTitle(R.string.crash_dialog_title)
            .setResCommentPrompt(R.string.crash_dialog_comment_prompt)
            .setResPositiveButtonText(android.R.string.ok);
        configurationBuilder
            .getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class)
            .setEnabled(true)
            .setMailTo("bug-reports@myexpenses.mobi");
      }
      return configurationBuilder.build();
    } catch (ACRAConfigurationException e) {
      Timber.e(e, "ACRA not initialized");
      return null;
    }
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
}
