package org.totschnig.myexpenses.feature

import android.app.Activity
import android.app.Application
import android.content.Context

interface FeatureManager {
    enum class Feature {
        OCR;
    }
    fun initApplication(application: Application)
    fun initActivity(activity: Activity)
    fun isFeatureInstalled(feature: Feature): Boolean
    fun requestFeature(feature: Feature)
    fun requestLocale(context: Context)
    fun registerCallback(callback: Callback)
    fun unregister()
}

interface Callback {
    fun onAvailable()
    fun onAsyncStarted(feature: FeatureManager.Feature) {}
    fun onAsyncStarted(displayLanguage: String) {}
    fun onError(throwable: Throwable)
}