package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import org.threeten.bp.LocalDate
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

class DebtEdit : EditActivity(), ButtonWithDialog.Host {
    private lateinit var binding: OneDebtBinding
    val viewModel: DebtViewModel by viewModels()
    val currencyViewModel: CurrencyViewModel by viewModels()

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
                        binding.Label.setText(it.label)
                        binding.Description.setText(it.description)
                        binding.Amount.setSelectedCurrency(it.currency)
                        binding.Amount.setAmount(Money(currencyContext[it.currency], it.amount).amountMajor)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (debtId != 0L) {
            menuInflater.inflate(R.menu.debt_edit, menu)
        }
        return true
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        return when(command) {
            R.id.CLOSE_DEBT_COMMAND -> {
                viewModel.closeDebt(debtId).observe(this) {
                    if (it) {
                        finish()
                    } else {
                        showSnackbar("ERROR")
                    }
                }
                true
            }
            else -> false
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
            binding.Label.error = getString(R.string.no_title_given)
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