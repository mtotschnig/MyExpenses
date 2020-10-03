package org.totschnig.myexpenses.util.crashreporting;

import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class AcraCrashHandler extends CrashHandler {

  @Override
  public void onAttachBaseContext(MyApplication application) {
    CoreConfigurationBuilder configurationBuilder = new CoreConfigurationBuilder(application)
        .setEnabled(true)
        .setBuildConfigClass(BuildConfig.class);
    configurationBuilder
        .setReportFormat(StringFormat.KEY_VALUE_LIST)
        .setReportField(ReportField.APP_VERSION_CODE, true)
        .setReportField(ReportField.USER_CRASH_DATE, true)
        .setExcludeMatchingSharedPreferencesKeys("planner_calendar_path", "password")
        .getPluginConfigurationBuilder(DialogConfigurationBuilder.class)
        .setEnabled(true)
        .setResText(R.string.crash_dialog_text)
        .setTitle(Utils.getTextWithAppName(application, R.string.crash_dialog_title).toString())
        .setResCommentPrompt(R.string.crash_dialog_comment_prompt)
        .setResPositiveButtonText(android.R.string.ok);
    configurationBuilder
        .getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class)
        .setEnabled(true)
        .setReportAsFile(false)
        .setMailTo("bug-reports@myexpenses.mobi");
    try {
      ACRA.init(application, configurationBuilder.build());
    } catch (ACRAConfigurationException e) {
      Timber.e(e);
    }
  }

  @Override
  void setupLoggingDo(Context context) {
    setKeys(context);
  }

  @Override
  public void putCustomData(@NonNull String key, String value) {
    ACRA.getErrorReporter().putCustomData(key, value);
  }
}
