package org.totschnig.myexpenses.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.totschnig.myexpenses.R


@RunWith(RobolectricTestRunner::class)
class AppNameLocalizationTest {

    @Test
    fun shouldBuildWithAppName() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val locales = context.resources.getStringArray(R.array.pref_ui_language_values).asList()
            .minus("default")
        val failures = mutableListOf<Pair<String, String>>()
        for (locale in locales) {
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
                R.string.crash_reports_user_info,
                R.string.notifications_permission_required_planner
            )) {
                try {
                    Utils.getTextWithAppName(context, resId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    failures.add(locale to context.resources.getResourceName(resId))
                }
            }
        }
        if (failures.size > 0) {
            Assert.fail("Non-compliant resources: " + failures.joinToString()
            )
        }
    }

    @Test
    fun shouldBuildTellAFriendMessage() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val locales = context.resources.getStringArray(R.array.pref_ui_language_values).asList()
            .minus("default")
        val failures = mutableListOf<String>()
        for (locale in locales) {
            setLocale(locale)
            try {
                Utils.getTellAFriendMessage(context)
            } catch (e: Exception) {
                failures.add(locale)
            }
        }
        if (failures.size > 0) {
            Assert.fail("Non-compliant resources: " + failures.joinToString()
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
}