package org.totschnig.myexpenses.util.ads

import android.content.Context
import android.view.ViewGroup
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity

interface AdHandlerFactory {
    val isRequestLocationInEeaOrUnknown: Boolean
    val isAdDisabled: Boolean
    fun create(adContainer: ViewGroup, baseActivity: BaseActivity): AdHandler

    /**
     * @param context   context in which callback action will be taken
     * @param forceShow if false, consent form is only shown if consent is unknown
     */
    fun gdprConsent(context: ProtectedFragmentActivity, forceShow: Boolean)
    fun clearConsent()
    fun setConsent(context: Context?, personalized: Boolean)
}