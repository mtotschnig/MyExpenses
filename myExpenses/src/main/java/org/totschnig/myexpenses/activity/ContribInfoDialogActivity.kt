package org.totschnig.myexpenses.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ContribDialogFragment
import org.totschnig.myexpenses.dialog.DonateDialogFragment
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
    private var doFinishAfterMessageDismiss = true
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

    private val packageFromExtra: Package?
        get() {
            return intent.getParcelableExtra(KEY_PACKAGE)
        }

    private fun contribBuyGithub(aPackage: Package) {
        val paymentOptions = licenceHandler.getPaymentOptions(aPackage)
        if (paymentOptions.size > 1) {
            DonateDialogFragment.newInstance(aPackage).show(supportFragmentManager, "CONTRIB")
        } else {
            startPayment(paymentOptions[0], aPackage)
        }
    }

    fun contribBuyDo(aPackage: Package) {
        val bundle = Bundle(1)
        bundle.putString(Tracker.EVENT_PARAM_PACKAGE, aPackage.javaClass.simpleName)
        logEvent(Tracker.EVENT_CONTRIB_DIALOG_BUY, bundle)
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

    override val snackBarContainerId: Int
        get() = android.R.id.content

    fun startPayment(paymentOption: Int, aPackage: Package) {
        if (paymentOption == R.string.donate_button_paypal) {
            try {
                startActivityForResult(Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = Uri.parse(licenceHandler.getPaypalUri(aPackage))
                }, PAYPAL_REQUEST)
            } catch (e: ActivityNotFoundException) {
                complain("No activity found for opening Paypal")
            }
        } else if (paymentOption == R.string.donate_button_invoice) {
            sendInvoiceRequest(aPackage)
        }
    }

    override fun onMessageDialogDismissOrCancel() {
        if (doFinishAfterMessageDismiss) {
            finish(true)
        }
    }

    fun finish(canceled: Boolean) {
        val featureStringFromExtra = intent.getStringExtra(KEY_FEATURE)
        if (featureStringFromExtra != null) {
            val feature = ContribFeature.valueOf(featureStringFromExtra)
            val usagesLeft = feature.usagesLeft(prefHandler)
            val shouldCallFeature =
                licenceHandler.hasAccessTo(feature) || !canceled && usagesLeft > 0
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

    override fun onLicenceStatusSet(newStatus: String?) {
        if (newStatus != null) {
            Timber.d("Purchase is premium upgrade. Congratulating user.")
            showMessage(
                String.format(
                    "%s (%s) %s", getString(R.string.licence_validation_premium),
                    newStatus, getString(R.string.thank_you)
                )
            )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == PAYPAL_REQUEST || requestCode == INVOICE_REQUEST) {
            finish(false)
        }
    }

    override val shouldQueryIap: Boolean
        get() = false

    companion object {
        const val KEY_FEATURE = "feature"
        private const val KEY_PACKAGE = "package"
        const val KEY_TAG = "tag"
        private const val KEY_SHOULD_REPLACE_EXISTING = "shouldReplaceExisting"
        fun getIntentFor(context: Context?, feature: ContribFeature?) =
            Intent(context, ContribInfoDialogActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                if (feature != null) {
                    putExtra(KEY_FEATURE, feature.name)
                }
            }

        fun getIntentFor(
            context: Context?,
            aPackage: Package,
            shouldReplaceExisting: Boolean
        ) = Intent(context, ContribInfoDialogActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra(KEY_PACKAGE, aPackage)
            putExtra(KEY_SHOULD_REPLACE_EXISTING, shouldReplaceExisting)
        }
    }
}