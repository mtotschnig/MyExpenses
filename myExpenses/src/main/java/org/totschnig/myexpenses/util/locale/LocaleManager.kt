package org.totschnig.myexpenses.util.locale

import android.app.Activity
import android.app.Application
import android.content.Context
import java.lang.Exception

interface LocaleManager {
    fun initApplication(application: Application)
    fun initActivity(activity: Activity)
    fun requestLocale(context: Context)
    fun onResume(callback: Callback)
    fun onPause()
}

interface Callback {
    fun onAvailable()
    fun onAsyncStarted(displayLanguage: String)
    fun onError(exception: Exception)
}