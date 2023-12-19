package org.totschnig.myexpenses.util.ads

import android.content.Context
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.LicenceHandler

@Suppress("unused")
open class
DefaultAdHandlerFactory(
    protected val context: Context,
    protected val prefHandler: PrefHandler,
    protected val userCountry: String,
    private val licenceHandler: LicenceHandler
) : AdHandlerFactory {
    override val isAdDisabled: Boolean
        get() = !prefHandler.getBoolean(PrefKey.DEBUG_ADS, false) &&
                (licenceHandler.hasAccessTo(ContribFeature.AD_FREE) ||
                        isInInitialGracePeriod || BuildConfig.DEBUG)
    private val isInInitialGracePeriod: Boolean
        get() = Utils.getDaysSinceInstall(context) < INITIAL_GRACE_DAYS

    companion object {
        private const val INITIAL_GRACE_DAYS = 2
    }
}