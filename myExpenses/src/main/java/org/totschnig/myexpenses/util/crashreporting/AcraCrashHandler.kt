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
import org.totschnig.myexpenses.util.Utils

class AcraCrashHandler : CrashHandler() {
    override fun onAttachBaseContext(application: MyApplication) {
        application.initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            excludeMatchingSharedPreferencesKeys = listOf("planner_calendar_path", "password")
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

    override fun setupLoggingDo(context: Context) {
        setKeys(context)
    }

    override fun putCustomData(key: String, value: String?) {
        value?.let { errorReporter.putCustomData(key, it) }
    }
}