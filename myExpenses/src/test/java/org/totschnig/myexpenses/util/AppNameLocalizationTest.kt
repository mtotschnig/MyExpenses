package org.totschnig.myexpenses.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.squareup.phrase.Phrase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.totschnig.myexpenses.R

@RunWith(ParameterizedRobolectricTestRunner::class)
class AppNameLocalizationTest(private val locale: String) {
    @Test
    fun shouldBuildWithAppName() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        setLocale(locale)
        for (resId in intArrayOf(
            R.string.dialog_contrib_reminder_remove_limitation,
            R.string.dialog_contrib_text_1,
            R.string.dialog_contrib_text_2,
            R.string.dialog_remind_rate_how_many_stars,
            R.string.dialog_remind_rate_1,
            R.string.plan_calendar_name,
            R.string.calendar_permission_required,
            R.string.description_webdav_url,
            R.string.warning_synchronization_folder_usage,
            R.string.onboarding_ui_title,
            R.string.crash_dialog_title,
            R.string.crash_reports_user_info
        )) {
            try {
                Utils.getTextWithAppName(context, resId)
            } catch (e: Exception) {
                Assert.fail(
                    String.format(
                        "Non-compliant resource %s for locale %s",
                        context.resources.getResourceName(resId),
                        locale
                    )
                )
            }
        }
    }

    @Test
    fun shouldBuildTellAFriendMessage() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        setLocale(locale)
        Utils.getTellAFriendMessage(context)
    }

    @Test
    fun shouldBuildWithPhrase() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        setLocale(locale)
        try {
            Phrase.from(context, R.string.gdpr_consent_message)
                .put(Utils.PLACEHOLDER_APP_NAME, context.getString(R.string.app_name))
                .put("ad_provider", "PubNative")
                .format()
        } catch (e: Exception) {
            Assert.fail(
                String.format(
                    "Non-compliant resource gdpr_consent_message for locale %s",
                    locale
                )
            )
        }
    }

    private fun setLocale(locale: String) {
        RuntimeEnvironment.setQualifiers(mapToQualifier(locale))
    }

    private fun mapToQualifier(locale: String): String {
        val parts = locale.split("-").toTypedArray()
        return if (parts.size == 2) {
            parts[0] + "-r" + parts[1]
        } else locale
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Locale: {0}")
        fun params() = ApplicationProvider.getApplicationContext<Application>().resources
            .getStringArray(R.array.pref_ui_language_values).asList().minus("default")
    }
}