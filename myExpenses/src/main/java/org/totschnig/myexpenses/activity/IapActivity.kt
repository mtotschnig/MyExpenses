package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.MyApplication.INVOICES_EMAIL
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.BillingListener
import org.totschnig.myexpenses.util.licence.BillingManager
import org.totschnig.myexpenses.util.licence.Package
import java.util.*

abstract class IapActivity: ProtectedFragmentActivity(), BillingListener {
    var billingManager: BillingManager? = null
    abstract val shouldQueryIap: Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = licenceHandler.initBillingManager(this, shouldQueryIap)
    }

    override fun onResume() {
        super.onResume()
        billingManager?.onResume(shouldQueryIap)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager?.destroy()
        billingManager = null
    }

    fun sendInvoiceRequest(aPackage: Package) {
        val packageLabel = licenceHandler.getButtonLabel(aPackage)
        val subject = "[${getString(R.string.app_name)}] ${getString(R.string.request_for_invoice)}"
        val userCountry = Utils.getCountryFromTelephonyManager(this)
        val messageBody =
            "${getString(R.string.licence_key)}: $packageLabel\n${getString(R.string.full_name)}:\n${
                getString(R.string.postal_country)
            }: ${userCountry ?: ""}"
        sendEmail(INVOICES_EMAIL, subject, messageBody, INVOICE_REQUEST)
    }

    fun onPurchaseCancelled() {
        showMessage(getString(R.string.premium_failed_or_canceled))
    }

    fun onPurchaseFailed(code: Int) {
        showMessage(
            String.format(
                Locale.ROOT,
                "%s (%d)",
                getString(R.string.premium_failed_or_canceled),
                code
            )
        )
    }
}