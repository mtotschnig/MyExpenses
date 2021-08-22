package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import org.threeten.bp.LocalDate
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OneDebtBinding
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.ButtonWithDialog
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt

class DebtEdit : EditActivity(), ButtonWithDialog.Host {
    private lateinit var binding: OneDebtBinding
    val viewModel: DebtViewModel by viewModels()
    val currencyViewModel: CurrencyViewModel by viewModels()

    val payeeName: String
        get() = intent.getStringExtra(DatabaseConstants.KEY_PAYEE_NAME)!!

    val payeeId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_PAYEEID, 0)

    val debtId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_DEBT_ID, 0)

    override fun getDiscardNewMessage(): Int {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneDebtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupToolbar(true)
        with((application as MyApplication).appComponent) {
            inject(viewModel)
            inject(currencyViewModel)
        }
        currencyViewModel.getCurrencies().observe(this) {
            binding.Amount.setCurrencies(it, currencyContext)
        }
        if (savedInstanceState == null) {
            if (debtId == 0L) {
                binding.DateButton.setDate(LocalDate.now())
                setTitle(false)
            } else {
                //load debt
            }
        }
        binding.Amount.setTypeChangedListener {
            setTitle(it)
        }
    }

    override fun saveState() {
        binding.Amount.selectedCurrency?.let {
            viewModel.saveDebt(
                Debt(debtId, binding.Label.text.toString(), binding.Description.text.toString(), payeeId,
                    binding.Amount.typedValue, currencyContext[it.code], binding.DateButton.date)
            ).observe(this) {
                finish()
            }
        }
    }

    fun setTitle(signum: Boolean) {
        title = getString(if (signum) R.string.debt_owes_me else R.string.debt_I_owe, payeeName)
    }

    override fun onValueSet(view: View) {}
}