package org.totschnig.myexpenses.util.ads

import android.app.Activity

interface AdHandlerFactory {
    suspend fun isPrivacyOptionsRequired(activity: Activity) = false
    val isAdDisabled: Boolean
        get() = true

    val isInitialized: Boolean
        get() = true

    fun createV2(): AdHandlerV2 = NoOpAdHandler

    /**
     * @param context   context in which callback action will be taken
     * @param forceShow if false, consent form is only shown if consent is unknown
     */
    fun gdprConsent(context: Activity, forceShow: Boolean) {}
}
