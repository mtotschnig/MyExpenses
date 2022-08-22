package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.viewModels
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OneDebtBinding
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.ButtonWithDialog
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.time.LocalDate

class DebtEdit : EditActivity(), ButtonWithDialog.Host {
    private lateinit var binding: OneDebtBinding
    private val viewModel: DebtViewModel by viewModels()
    private val currencyViewModel: CurrencyViewModel by viewModels()

    val payeeName: String
        get() = intent.getStringExtra(DatabaseConstants.KEY_PAYEE_NAME)!!

    val payeeId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_PAYEEID, 0)

    private val debtId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_DEBT_ID, 0)

    override fun getDiscardNewMessage() = R.string.dialog_confirm_discard_new_debt

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
        currencyViewModel.getCurrencies().observe(this) { list ->
            binding.Amount.setCurrencies(list)
            if (savedInstanceState == null) {
                if (debtId != 0L) {
                    viewModel.loadDebt(debtId).observe(this) {
                        if (it.isSealed) {
                            setResult(RESULT_FIRST_USER)
                            finish()
                        }
                        binding.Label.setText(it.label)
                        binding.Description.setText(it.description)
                        binding.Amount.setSelectedCurrency(it.currency)
                        binding.Amount.setAmount(Money(it.currency, it.amount).amountMajor)
                        binding.DateButton.setDate(epoch2ZonedDateTime(it.date).toLocalDate())
                        setTitle(it.amount > 0)
                        setupListeners()
                    }
                } else {
                    binding.Amount.setSelectedCurrency(Utils.getHomeCurrency())
                }
            }
        }
        if (savedInstanceState == null) {
            if (debtId == 0L) {
                binding.DateButton.setDate(LocalDate.now())
                setTitle(false)
            }
        }
        if (savedInstanceState != null || debtId == 0L) {
            setupListeners()
        }
        if (debtId != 0L) {
            binding.Amount.disableCurrencySelection()
            mNewInstance = false
        }
        binding.Amount.setTypeChangedListener {
            setTitle(it)
        }
    }

    private fun setupListeners() {
        binding.Amount.addTextChangedListener(this)
        binding.Label.addTextChangedListener(this)
        binding.Description.addTextChangedListener(this)
    }

    override fun saveState() {
        val label = binding.Label.text.toString()
        if (label == "") {
            binding.Label.error = getString(R.string.required)
            return
        }
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
}