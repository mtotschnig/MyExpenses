package org.totschnig.myexpenses.util.ads

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.squareup.phrase.Phrase
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.dialog.GdprDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import java.util.*

open class DefaultAdHandlerFactory(
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
    override val isRequestLocationInEeaOrUnknown: Boolean
        get() = Arrays.binarySearch(EU_COUNTRIES, userCountry) >= 0

    override fun create(adContainer: ViewGroup, baseActivity: BaseActivity): AdHandler {
        return if (isAdDisabled) NoOpAdHandler else CustomAdHandler(
            this,
            adContainer,
            baseActivity,
            userCountry
        )
    }

    override fun gdprConsent(context: ProtectedFragmentActivity, forceShow: Boolean) {
        if (forceShow || (!isAdDisabled && isRequestLocationInEeaOrUnknown &&
                    !prefHandler.isSet(PrefKey.PERSONALIZED_AD_CONSENT))
        ) {
            context.lifecycleScope.launch {
                context.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    GdprDialogFragment().show(context.supportFragmentManager, "GDPR")
                }
            }
        }
    }

    override fun clearConsent() {
        prefHandler.remove(PrefKey.PERSONALIZED_AD_CONSENT)
    }

    override fun setConsent(context: Context?, personalized: Boolean) {
        prefHandler.putBoolean(PrefKey.PERSONALIZED_AD_CONSENT, personalized)
    }

    companion object {
        private const val INITIAL_GRACE_DAYS = 2
        private val EU_COUNTRIES = arrayOf(
            "at",
            "be",
            "bg",
            "cy",
            "cz",
            "de",
            "dk",
            "ee",
            "el",
            "es",
            "fi",
            "fr",
            "hr",
            "hu",
            "ie",
            "it",
            "lt",
            "lu",
            "lv",
            "mt",
            "nl",
            "pl",
            "pt",
            "ro",
            "se",
            "si",
            "sk",
            "uk"
        )
    }
}