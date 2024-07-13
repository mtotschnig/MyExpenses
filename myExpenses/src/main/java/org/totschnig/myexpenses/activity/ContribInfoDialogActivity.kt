package org.totschnig.myexpenses.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.evernote.android.state.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ContribDialogFragment
import org.totschnig.myexpenses.dialog.DonateDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.distrib.DistributionHelper.distribution
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isGithub
import org.totschnig.myexpenses.util.distrib.DistributionHelper.isPlay
import org.totschnig.myexpenses.util.licence.Package
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.tracking.Tracker
import timber.log.Timber


/**
 * Manages the dialog shown to user when they request usage of a premium functionality or click on
 * the dedicated entry on the preferences screen. If called from an activity extending
 * [ProtectedFragmentActivity], [ContribIFace.contribFeatureCalled]
 * or [ContribIFace.contribFeatureNotCalled] will be triggered on it, depending on
 * if user canceled or has usages left. If called from shortcut, this activity will launch the intent
 * for the premium feature directly
 */
class ContribInfoDialogActivity : IapActivity() {
    @State
    var doFinishAfterMessageDismiss = true
    @State
    var purchaseStarted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            packageFromExtra?.also {
                if (isGithub) {
                    contribBuyDo(it)
                }
            } ?: run {
                ContribDialogFragment.newInstance(
                    intent.getStringExtra(KEY_FEATURE),
                    intent.getSerializableExtra(KEY_TAG)
                )
                    .show(supportFragmentManager, "CONTRIB")
                supportFragmentManager.executePendingTransactions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (purchaseStarted) {
            if (intent.action == ACTION_FINISH) {
                showMessage(
                    "${getString(R.string.paypal_callback_info)} ${intent.getStringExtra(KEY_MESSAGE)}"
                )
            } else {
                finish(true)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent.also {
            it.putExtra(KEY_FEATURE, this.intent.getStringExtra(KEY_FEATURE))
            it.putExtra(KEY_TAG, this.intent.getSerializableExtra(KEY_TAG))
        })
    }

    private val packageFromExtra: Package?
        get() {
            return intent.getParcelableExtra(KEY_PACKAGE)
        }

    private fun contribBuyGithub(aPackage: Package) {
        val paymentOptions = licenceHandler.getPaymentOptions(aPackage, injector.userCountry())
        if (paymentOptions.size > 1) {
            DonateDialogFragment.newInstance(aPackage).show(supportFragmentManager, "CONTRIB")
        } else {
            startPayment(paymentOptions[0], aPackage)
        }
    }

    fun contribBuyDo(aPackage: Package) {
        logEvent(Tracker.EVENT_CONTRIB_DIALOG_BUY, Bundle(1).apply {
            putString(Tracker.EVENT_PARAM_PACKAGE, aPackage.javaClass.simpleName)
        })
        when (distribution) {
            DistributionHelper.Distribution.PLAY, DistributionHelper.Distribution.AMAZON ->
                lifecycleScope.launch {
                    try {
                        licenceHandler.launchPurchase(
                            aPackage,
                            intent.getBooleanExtra(KEY_SHOULD_REPLACE_EXISTING, false),
                            billingManager!!
                        )
                    } catch (e: IllegalStateException) {
                        if (e !is CancellationException) {
                            report(e)
                            showMessage(e.safeMessage)
                        }
                    }
                }

            else -> contribBuyGithub(aPackage)
        }
    }

    fun complain(message: String) {
        report(Exception("**** InAppPurchase Error: $message"))
        showMessage(message)
    }

    fun startPayment(paymentOption: Int, aPackage: Package) {
        if (paymentOption == R.string.donate_button_paypal) {
            purchaseStarted = true
            val intent: CustomTabsIntent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this, Uri.parse(licenceHandler.getPaypalUri(aPackage)))
        } else if (paymentOption == R.string.donate_button_invoice) {
            sendInvoiceRequest(aPackage)
            finish()
        }
    }

    override fun onMessageDialogDismissOrCancel() {
        if (doFinishAfterMessageDismiss) {
            finish(true)
        }
    }

    override fun onPurchaseCancelled() {
        finish(true)
    }

    fun finish(canceled: Boolean) {
        val featureStringFromExtra = intent.getStringExtra(KEY_FEATURE)
        if (featureStringFromExtra != null) {
            val feature = ContribFeature.valueOf(featureStringFromExtra)
            val shouldCallFeature =
                licenceHandler.hasAccessTo(feature) ||
                        (!canceled && licenceHandler.usagesLeft(feature))
            if (callerIsContribIface()) {
                val i = Intent()
                i.putExtra(KEY_FEATURE, featureStringFromExtra)
                i.putExtra(KEY_TAG, intent.getSerializableExtra(KEY_TAG))
                if (shouldCallFeature) {
                    setResult(RESULT_OK, i)
                } else {
                    setResult(RESULT_CANCELED, i)
                }
            } else if (shouldCallFeature) {
                callFeature(feature)
            }
        }
        super.finish()
    }

    private fun callFeature(feature: ContribFeature) {
        if (feature === ContribFeature.SPLIT_TRANSACTION) {
            startActivity(ShortcutHelper.createIntentForNewSplit(this))
        }
        // else User bought licence in the meantime
    }

    private fun callerIsContribIface(): Boolean {
        var result = false
        val callingActivity = callingActivity
        if (callingActivity != null) {
            try {
                val caller = Class.forName(callingActivity.className)
                result = ContribIFace::class.java.isAssignableFrom(caller)
            } catch (ignored: ClassNotFoundException) {
            }
        }
        return result
    }

    override fun onLicenceStatusSet(success: Boolean, newStatus: String?) {
        if (success) {
            Timber.d("Purchase is premium upgrade. Congratulating user.")
            showMessage(
                String.format(
                    "%s (%s) %s", getString(R.string.licence_validation_premium),
                    newStatus, getString(R.string.thank_you)
                )
            )
        } else if (newStatus != null) {
            showMessage(newStatus)
        } else {
            complain("Validation of purchase failed")
        }
    }

    override fun onBillingSetupFinished() {
        packageFromExtra?.let { contribBuyDo(it) }
    }

    override fun onBillingSetupFailed(reason: String) {
        if (isPlay) {
            doFinishAfterMessageDismiss = false
            complain(String.format("Billing setup failed (%s)", reason))
        }
    }

    override val shouldQueryIap: Boolean
        get() = false

    companion object {
        const val KEY_FEATURE = "feature"
        private const val KEY_PACKAGE = "package"
        const val KEY_TAG = "tag"
        private const val KEY_SHOULD_REPLACE_EXISTING = "shouldReplaceExisting"
        private const val ACTION_FINISH = "FINISH"
        private const val KEY_MESSAGE = "message"

        fun getOnPurchaseCompleteIntent(context: Context, message: String) =
            Intent(context, ContribInfoDialogActivity::class.java).apply {
                action = ACTION_FINISH
                putExtra(KEY_MESSAGE, message)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }


        fun getIntentFor(context: Context?, feature: ContribFeature?) =
            Intent(context, ContribInfoDialogActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                if (feature != null) {
                    putExtra(KEY_FEATURE, feature.name)
                }
            }

        fun getIntentFor(
            context: Context,
            aPackage: Package,
            shouldReplaceExisting: Boolean
        ) = Intent(context, ContribInfoDialogActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra(KEY_PACKAGE, aPackage)
            putExtra(KEY_SHOULD_REPLACE_EXISTING, shouldReplaceExisting)
        }
    }
}