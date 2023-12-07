package org.totschnig.myexpenses.util.ads

import android.app.Activity
import android.view.ViewGroup
import org.totschnig.myexpenses.activity.BaseActivity

interface AdHandlerFactory {
    suspend fun isPrivacyOptionsRequired(activity: Activity) = false
    val isAdDisabled: Boolean
        get() = true

    fun create(adContainer: ViewGroup, baseActivity: BaseActivity): AdHandler = NoOpAdHandler

    /**
     * @param context   context in which callback action will be taken
     * @param forceShow if false, consent form is only shown if consent is unknown
     */
    fun gdprConsent(context: Activity, forceShow: Boolean) {}
}