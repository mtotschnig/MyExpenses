package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.BillingListener
import org.totschnig.myexpenses.util.licence.BillingManager
import org.totschnig.myexpenses.util.licence.Package

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
        val mailto = "mailto:${MyApplication.INVOICES_EMAIL}&subject=${Uri.encode(subject)}&body=${
            Uri.encode(messageBody)
        }"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(mailto)
        }
        startActivity(intent, R.string.no_app_handling_email_available, INVOICE_REQUEST)
    }
}