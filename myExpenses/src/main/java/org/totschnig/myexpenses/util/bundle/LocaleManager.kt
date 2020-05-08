package org.totschnig.myexpenses.util.bundle

import android.app.Activity
import android.app.Application
import android.content.Context

interface LocaleManager {
    fun initApplication(application: Application)
    fun initActivity(activity: Activity)
    fun requestLocale(context: Context)
    fun onResume(onAvailable: () -> Unit)
    fun onPause()
}