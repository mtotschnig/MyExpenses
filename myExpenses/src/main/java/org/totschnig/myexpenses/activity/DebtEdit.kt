package org.totschnig.myexpenses.activity

import android.os.Bundle
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
import org.totschnig.myexpenses.util.epoch2ZonedDateTime
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.DebtViewModel
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.time.LocalDate
import kotlin.math.sign

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENCY, binding.Amount.selectedCurrency?.code)
    }

    override val fabActionName = "SAVE_DEBT"

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setTitle(binding.Amount.type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OneDebtBinding.inflate(layoutInflater)
        setContentView(binding.root)
        floatingActionButton = binding.fab.CREATECOMMAND
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
                        setSelectedCurrency(currencyContext[it])
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
            newInstance = false
        }
        binding.Amount.setTypeChangedListener {
            setTitle(it)
        }
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
        binding.Amount.selectedCurrency?.let { currency ->
            val currencyUnit = currencyContext[currency.code]
            binding.Amount.getAmount(currencyUnit).getOrNull()?.let { amount ->
                val equivalentAmount = if (currency.code == homeCurrency.code) null else
                    (binding.EquivalentAmount.getAmount(homeCurrency).getOrNull()
                        ?: return).let {
                        if (amount.amountMinor.sign == -1) it.negate() else it
                    }
                isSaving = true
                viewModel.saveDebt(
                    Debt(
                        id = debtId,
                        label = binding.Label.text.toString(),
                        description = binding.Description.text.toString(),
                        payeeId = payeeId,
                        amount = amount.amountMinor,
                        currency = currencyUnit,
                        date = binding.DateButton.date,
                        equivalentAmount = equivalentAmount?.amountMinor
                    )
                ).observe(this) {
                    finish()
                }
            }
        }
    }

    fun setTitle(signum: Boolean) {
        title = getString(if (signum) R.string.debt_owes_me else R.string.debt_I_owe, payeeName)
    }

    override val date: LocalDate
        get() = binding.DateButton.date

}