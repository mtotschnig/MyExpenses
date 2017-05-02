package org.totschnig.myexpenses.di;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.HashLicenceHandler;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

@Module
public class AppModule {
  private MyApplication application;

  public AppModule(MyApplication application) {
    this.application = application;
  }

  @Provides
  @Singleton
  LicenceHandler providesLicenceHandler() {
    return new HashLicenceHandler(application);
  }

  @Provides
  @Singleton
  @Nullable
  ACRAConfiguration providesAcraConfiguration() {
    if (MyApplication.isInstrumentationTest()) return null;
    try {
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(application);
      if (AcraHelper.DO_REPORT) {
        configurationBuilder.setFormUri(BuildConfig.ACRA_FORM_URI)
            .setReportType(HttpSender.Type.JSON)
            .setHttpMethod(HttpSender.Method.PUT)
            .setFormUriBasicAuthLogin(BuildConfig.ACRA_FORM_URI_BASIC_AUTH_LOGIN)
            .setFormUriBasicAuthPassword(BuildConfig.ACRA_FORM_URI_BASIC_AUTH_PASSWORD)
            .setLogcatArguments("-t", "250", "-v", "long", "ActivityManager:I", "MyExpenses:V", "*:S")
            .setExcludeMatchingSharedPreferencesKeys("planner_calendar_path", "password");
      } else {
        configurationBuilder.setReportingInteractionMode(ReportingInteractionMode.DIALOG)
            .setMailTo("bug-reports@myexpenses.mobi")
            .setResDialogText(R.string.crash_dialog_text)
            .setResDialogTitle(R.string.crash_dialog_title)
            .setResDialogCommentPrompt(R.string.crash_dialog_comment_prompt);
      }
      return configurationBuilder.build();
    } catch (ACRAConfigurationException e) {
      Timber.e(e, "ACRA not initialized");
      return null;
    }
  }

  @Provides
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
          //noop
        }

        @Override
        public void setEnabled(boolean enabled) {
          //noop
        }
      };
    }
  }
}
