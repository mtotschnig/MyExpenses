package org.totschnig.myexpenses.feature

import android.app.Activity
import android.app.Application
import android.content.Context
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler

const val OCR_MODULE = "ocr"

interface FeatureManager {
    fun initApplication(application: Application)
    fun initActivity(activity: Activity)
    fun isFeatureInstalled(feature: String, context: Context, prefHandler: PrefHandler): Boolean
    fun requestFeature(feature: String, activity: BaseActivity)
    fun requestLocale(context: Context)
    fun registerCallback(callback: Callback)
    fun unregister()
    fun allowsUninstall() = false
    fun installedFeatures(): Set<String> = emptySet()
    fun installedLanguages(): Set<String> = emptySet()
    fun uninstallFeatures(features: Set<String>) {}
    fun uninstallLanguages(languages: Set<String>) {}
}

interface Callback {
    fun onAvailable()
    fun onAsyncStartedFeature(feature: String) {}
    fun onAsyncStartedLanguage(displayLanguage: String) {}
    fun onError(throwable: Throwable)
}