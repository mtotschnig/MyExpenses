package org.totschnig.myexpenses.util;

import android.app.Application;

import com.squareup.phrase.Phrase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.totschnig.myexpenses.R;

import androidx.test.core.app.ApplicationProvider;

import static junit.framework.Assert.fail;
import static org.totschnig.myexpenses.util.Utils.PLACEHOLDER_APP_NAME;

@RunWith(RobolectricTestRunner.class)
public class AppNameLocalizationTest {

  @Test
  public void shouldBuildWithAppName() {
    Application context = ApplicationProvider.getApplicationContext();
    String[] locales = getLocales(context);
    for (String locale : locales) {
      if (!locale.equals("default")) {
        setLocale(locale);
        for (int resId : new int[]{
            R.string.dialog_contrib_reminder_remove_limitation,
            R.string.dialog_contrib_text_1,
            R.string.dialog_contrib_text_2,
            R.string.dialog_remind_rate_how_many_stars,
            R.string.dialog_remind_rate_1,
            R.string.plan_calendar_name,
            R.string.warning_app_folder_will_be_deleted_upon_uninstall,
            R.string.calendar_permission_required,
            R.string.description_webdav_url,
            R.string.warning_synchronization_folder_usage,
            R.string.onboarding_ui_title,
            R.string.crash_dialog_title}) {
          try {
            Utils.getTextWithAppName(context, resId);
          } catch (Exception e) {
            fail(String.format("Non-compliant resource %s for locale %s", context.getResources().getResourceName(resId), locale));
          }
        }
      }
    }
  }

  public String[] getLocales(Application context) {
    return context.getResources().getStringArray(R.array.pref_ui_language_values);
  }

  @Test
  public void shouldBuildWithAppNameIfDefined() {
    Application context = ApplicationProvider.getApplicationContext();
    String[] locales = getLocales(context);
    for (String locale : locales) {
      if (!locale.equals("default")) {
        setLocale(locale);
        //These strings are not defined on every build flavor and branch
        for (String resName : new String[]{
            "crash_reports_user_info"
        }) {
          try {
            final int resId = context.getResources().getIdentifier(resName, "string", context.getPackageName());
            if (resId == 0) {
              continue;
            }
            Utils.getTextWithAppName(context, resId);
          } catch (Exception e) {
            fail(String.format("Non-compliant resource %s for locale %s", resName, locale));
          }
        }
      }
    }
  }

  @Test
  public void shouldBuildTellAFriendMessage() {
    Application context = ApplicationProvider.getApplicationContext();
    String[] locales = getLocales(context);
    for (String locale : locales) {
      if (!locale.equals("default")) {
        setLocale(locale);
        Utils.getTellAFriendMessage(context);
      }
    }
  }

  @Test
  public void shouldBuildWithPhrase() {
    Application context = ApplicationProvider.getApplicationContext();
    String[] locales = getLocales(context);
    for (String locale : locales) {
      if (!locale.equals("default")) {
        setLocale(locale);
        try {
          Phrase.from(context, R.string.gdpr_consent_message)
              .put(PLACEHOLDER_APP_NAME, context.getString(R.string.app_name))
              .put("ad_provider", "PubNative")
              .format();
        } catch (Exception e) {
          fail(String.format("Non-compliant resource gdpr_consent_message for locale %s", locale));
        }
      }
    }
  }

  private void setLocale(String locale) {
    RuntimeEnvironment.setQualifiers(mapToQualifier(locale));
  }

  private String mapToQualifier(String locale) {
    String[] parts = locale.split("-");
    if (parts.length == 2) {
      return parts[0] + "-r" + parts[1];
    }
    return locale;
  }
}
