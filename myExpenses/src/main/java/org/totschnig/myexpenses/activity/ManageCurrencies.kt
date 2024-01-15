package org.totschnig.myexpenses.activity

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.CurrencyListBinding
import org.totschnig.myexpenses.dialog.EditCurrencyDialog.Companion.newInstance

class ManageCurrencies : ProtectedFragmentActivity() {

    private lateinit var binding: CurrencyListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CurrencyListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        supportActionBar!!.setTitle(R.string.pref_custom_currency_title)
    }

    override val fabActionName = "CREATE_CURRENCY"

    override val _floatingActionButton: FloatingActionButton
        get() = binding.fab.CREATECOMMAND

    override fun onFabClicked() {
        super.onFabClicked()
        newInstance(null).show(supportFragmentManager, "NEW_CURRENCY")
    }
}