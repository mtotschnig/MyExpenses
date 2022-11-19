package org.totschnig.myexpenses.util.tracking

import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import com.google.firebase.analytics.FirebaseAnalytics
import org.totschnig.myexpenses.util.Preconditions
import org.totschnig.myexpenses.util.distrib.DistributionHelper.distribution
import org.totschnig.myexpenses.util.licence.LicenceStatus

@Keep
class PlatformTracker : Tracker {
    private var firebaseAnalytics: FirebaseAnalytics? = null
    override fun init(context: Context, licenceStatus: LicenceStatus?) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        firebaseAnalytics!!.setUserProperty("Distribution", distribution.name)
        firebaseAnalytics!!.setUserProperty("Licence", licenceStatus!!.name)
    }

    override fun logEvent(eventName: String, params: Bundle?) {
        Preconditions.checkNotNull(firebaseAnalytics)
        firebaseAnalytics!!.logEvent(eventName, params)
    }

    override fun setEnabled(enabled: Boolean) {
        firebaseAnalytics!!.setAnalyticsCollectionEnabled(enabled)
    }
}