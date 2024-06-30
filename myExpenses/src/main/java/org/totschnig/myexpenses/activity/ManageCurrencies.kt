package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.EditCurrencyDialog
import org.totschnig.myexpenses.fragment.CurrencyList

class ManageCurrencies : ProtectedFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWithFragment(savedInstanceState == null) {
            CurrencyList()
        }
        setupToolbar()
        supportActionBar!!.setTitle(R.string.pref_custom_currency_title)
    }

    override val fabActionName = "CREATE_CURRENCY"

    override fun onFabClicked() {
        super.onFabClicked()
        EditCurrencyDialog.newInstance(null).show(supportFragmentManager, "NEW_CURRENCY")
    }
}