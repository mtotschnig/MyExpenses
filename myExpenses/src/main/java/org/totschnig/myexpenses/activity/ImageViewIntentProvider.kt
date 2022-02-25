package org.totschnig.myexpenses.activity

import android.content.Intent
import android.app.Activity
import android.content.Context
import android.net.Uri
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import android.widget.Toast
import org.totschnig.myexpenses.util.safeMessage
import java.lang.Exception

interface ImageViewIntentProvider {
    fun getViewIntent(context: Context, pictureUri: Uri): Intent
    fun startViewIntent(activity: Activity, pictureUri: Uri) {
        try {
            activity.startActivity(getViewIntent(activity, pictureUri))
        } catch (e: Exception) {
            CrashHandler.report(e, "pictureUri", pictureUri.toString())
            Toast.makeText(activity, e.safeMessage, Toast.LENGTH_LONG).show()
        }
    }
}