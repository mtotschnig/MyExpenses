package org.totschnig.myexpenses.util.crashreporting

import android.content.Context
import org.acra.ACRA.errorReporter
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils

class AcraCrashHandler(prefHandler: PrefHandler) : BaseCrashHandler(prefHandler) {
    override fun onAttachBaseContext(application: MyApplication) {
        application.initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            excludeMatchingSharedPreferencesKeys = listOf(
                prefHandler.getKey(PrefKey.PLANNER_CALENDAR_PATH),
                prefHandler.getKey(PrefKey.SET_PASSWORD),
                prefHandler.getKey(PrefKey.EXPORT_PASSWORD),
                prefHandler.getKey(PrefKey.WEBUI_PASSWORD)
                )
            dialog {
                text = application.getString(R.string.crash_dialog_text)
                title = Utils.getTextWithAppName(application, R.string.crash_dialog_title).toString()
                commentPrompt = application.getString(R.string.crash_dialog_comment_prompt)
                positiveButtonText = application.getString(android.R.string.ok)
                reportDialogClass
            }
            mailSender {
                reportAsFile = false
                mailTo = "bug-reports@myexpenses.mobi"
            }
        }
    }

    override suspend fun setupLogging(context: Context) {
        setKeys(context)
    }

    override fun putCustomData(key: String, value: String) {
        errorReporter.putCustomData(key, value)
    }
}