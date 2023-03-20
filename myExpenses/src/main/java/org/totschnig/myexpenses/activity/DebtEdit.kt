package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.text.Editable
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filterNotNull
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OneDebtBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.ui.ButtonWithDialog
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.math.BigDecimal
import java.time.LocalDate

class DebtEdit : EditActivity(), ButtonWithDialog.Host, ExchangeRateEdit.Host {
    private lateinit var binding: OneDebtBinding
    private val viewModel: DebtViewModel by viewModels()
    private val currencyViewModel: CurrencyViewModel by viewModels()

    val payeeName: String
        get() = intent.getStringExtra(DatabaseConstants.KEY_PAYEE_NAME)!!

    val payeeId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_PAYEEID, 0)

    private val debtId: Long
        get() = intent.getLongExtra(DatabaseConstants.KEY_DEBT_ID, 0)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENCY, binding.Amount.selectedCurrency?.code)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setTitle(binding.Amount.type)
    }

    val homeCurrency: CurrencyUnit
        get() = Utils.getHomeCurrency()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneDebtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupToolbarWithClose()
        with((application as MyApplication).appComponent) {
            inject(viewModel)
            inject(currencyViewModel)
        }
        lifecycleScope.launchWhenStarted {

            currencyViewModel.currencies.collect { list ->
                binding.Amount.setCurrencies(list)
                if (savedInstanceState == null) {
                    if (debtId != 0L) {
                        viewModel.loadDebt(debtId).filterNotNull().collect { debt ->
                            if (debt.isSealed) {
                                setResult(RESULT_FIRST_USER)
                                finish()
                            }
                            binding.Label.setText(debt.label)
                            binding.Description.setText(debt.description)
                            setSelectedCurrency(debt.currency)
                            binding.Amount.setAmount(Money(debt.currency, debt.amount).amountMajor)
                            binding.DateButton.setDate(epoch2ZonedDateTime(debt.date).toLocalDate())
                            debt.equivalentAmount?.let {
                                binding.EquivalentAmount.setAmount(
                                    Money(homeCurrency, it).amountMajor
                                )
                            }
                            setTitle(debt.amount > 0)
                            setupListeners()
                        }
                    } else {
                        setSelectedCurrency(homeCurrency)
                    }
                } else {
                    savedInstanceState.getString(KEY_CURRENCY)?.let {
                        setSelectedCurrency(currencyContext.get(it))
                    }
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
        binding.Amount.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                binding.EquivalentAmount.setCompoundResultInput(
                    binding.Amount.validate(
                        false
                    )
                )
            }
        })
        binding.EquivalentAmount.setFractionDigits(homeCurrency.fractionDigits)
    }

    private fun setupListeners() {
        binding.Amount.addTextChangedListener(this)
        binding.Label.addTextChangedListener(this)
        binding.Description.addTextChangedListener(this)
    }

    private fun setSelectedCurrency(currencyUnit: CurrencyUnit) {
        binding.Amount.setSelectedCurrency(currencyUnit)
        onCurrencySelectionChanged(currencyUnit)
    }

    override fun onCurrencySelectionChanged(currencyUnit: CurrencyUnit) {
        val isHomeAccount = currencyUnit.code == homeCurrency.code
        binding.EquivalentAmountRow.isVisible = !isHomeAccount
        if (!isHomeAccount) {
            binding.EquivalentAmount.configureExchange(currencyUnit, homeCurrency)
        }
    }

    override fun saveState() {
        val label = binding.Label.text.toString()
        if (label == "") {
            binding.Label.error = getString(R.string.required)
            return
        }
        binding.Amount.selectedCurrency?.let {
            val amount = binding.Amount.typedValue
            val equivalentAmount = if (it.code == homeCurrency.code) null else {
                Money(
                    currencyUnit = homeCurrency,
                    amountMajor = binding.EquivalentAmount.validate(false)?.let {
                        if (amount.signum() == -1) it.negate() else it
                    } ?: BigDecimal.ZERO
                )
            }
            viewModel.saveDebt(
                Debt(
                    debtId,
                    binding.Label.text.toString(),
                    binding.Description.text.toString(),
                    payeeId,
                    amount,
                    currencyContext[it.code],
                    binding.DateButton.date,
                    equivalentAmount?.amountMinor
                )
            ).observe(this) {
                finish()
            }
        }
    }

    fun setTitle(signum: Boolean) {
        title = getString(if (signum) R.string.debt_owes_me else R.string.debt_I_owe, payeeName)
    }

    override fun getDate(): LocalDate {
        return binding.DateButton.date
    }
}