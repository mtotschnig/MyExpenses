package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.EditCurrencyDialog.Companion.newInstance

class ManageCurrencies : ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.currency_list)
        setupToolbar()
        supportActionBar!!.setTitle(R.string.pref_custom_currency_title)
    }

    override val fabActionName = "CREATE_CURRENCY"

    override fun onFabClicked() {
        super.onFabClicked()
        newInstance(null).show(supportFragmentManager, "NEW_CURRENCY")
    }
}