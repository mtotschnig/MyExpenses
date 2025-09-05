package org.totschnig.myexpenses.delegate

import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.evernote.android.state.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_NEGATIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_MESSAGE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_NEGATIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_POSITIVE_BUTTON_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_PREFKEY
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_TITLE
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_HINT_SHOWN
import org.totschnig.myexpenses.preference.shouldStartAutoFill
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.config.Configurator.Configuration.AUTO_COMPLETE_DROPDOWN_SET_INPUT_METHOD_NEEDED
import org.totschnig.myexpenses.util.config.get
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.ui.configurePopupAnchor
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt
import java.math.BigDecimal
import kotlin.math.sign

//Transaction or Split
abstract class MainDelegate<T : ITransaction>(
    viewBinding: OneExpenseBinding,
    dateEditBinding: DateEditBinding,
    methodRowBinding: MethodRowBinding,
    isTemplate: Boolean,
) : TransactionDelegate<T>(
    viewBinding,
    dateEditBinding,
    methodRowBinding,
    isTemplate
) {

    @State
    var originalAmountVisible = false

    @State
    var originalCurrencyCode: String? = null

    val userSetExchangeRate: BigDecimal?
        get() = viewBinding.EquivalentAmount.userSetExchangeRate

    private var debts: List<DisplayDebt> = emptyList()

    val payeeId
        get() = viewBinding.Payee.party?.id

    override fun bind(
        transaction: T?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean,
    ) {
        super.bind(
            transaction,
            withTypeSpinner,
            savedInstanceState,
            recurrence,
            withAutoFill
        )

        viewBinding.EquivalentAmount.addTextChangedListener(amountChangeWatcher)

        if (isSplitPart) {
            disableAccountSpinner()
            host.parentOriginalAmountExchangeRate?.let {
                originalAmountVisible = true
                originalCurrencyCode = it.second.code
                with(viewBinding.OriginalAmount) {
                    exchangeRate = it.first
                    disableCurrencySelection()
                    disableExchangeRateEdit()
                    requestFocus()
                }
            }
        }

        if (originalAmountVisible) {
            configureOriginalAmountVisibility()
        }
    }

    private fun configureOriginalAmountVisibility() {
        viewBinding.OriginalAmountRow.isVisible = originalAmountVisible
    }

    private fun populateOriginalCurrency() {
        viewBinding.OriginalAmount.setSelectedCurrency(
            originalCurrencyCode?.let { currencyContext[it] } ?: homeCurrency
        )
    }

    fun setCurrencies(currencies: List<Currency>) {
        viewBinding.OriginalAmount.setCurrencies(currencies)
        populateOriginalCurrency()
    }

    fun toggleOriginalAmount() {
        originalAmountVisible = !originalAmountVisible
        configureOriginalAmountVisibility()
        if (originalAmountVisible) {
            viewBinding.OriginalAmount.requestFocus()
        } else {
            viewBinding.OriginalAmount.clear()
        }
    }

    val originalAmountExchangeRate: Pair<BigDecimal, Currency>?
        get() {
            if (originalAmountVisible) {
                val exchangeRate = viewBinding.OriginalAmount.exchangeRate
                val currency = viewBinding.OriginalAmount.selectedCurrency
                if (exchangeRate != null && currency != null) {
                    return exchangeRate to currency
                }
            }
            return null
        }

    override fun configureType() {
        super.configureType()
        if (viewBinding.Amount.typedValue.compareTo(BigDecimal.ZERO) != 0) {
            onAmountChanged()
        }
    }

    override fun onAmountChanged() {
        super.onAmountChanged()
        if (debtId != null) {
            updateDebtCheckBox(debts.find { it.id == debtId })
        }
    }

    override fun buildTransaction(
        forSave: Boolean,
        account: Account,
    ): T? {
        val amount = validateAmountInput(forSave, currentAccount()!!.currency).getOrNull()
            ?: //Snackbar is shown in validateAmountInput
            return null
        return buildMainTransaction(account).apply {
            this.amount = amount
            if (!isSplitPart) {
                this.party = viewBinding.Payee.partyForSave
            }
            this.debtId = this@MainDelegate.debtId
            this.methodId = this@MainDelegate.methodId
            val selectedItem = viewBinding.OriginalAmount.selectedCurrency
            if (selectedItem != null) {
                val currency = selectedItem.code
                val originalAmount = viewBinding.OriginalAmount.getAmount(
                    currencyContext[currency]
                )
                originalAmount.onFailure {
                    return null
                }.onSuccess {
                    prefHandler.putString(PrefKey.LAST_ORIGINAL_CURRENCY, currency)
                    this.originalAmount = it
                }
            } else {
                this.originalAmount = null
            }
            val equivalentAmount = viewBinding.EquivalentAmount.getAmount(homeCurrency)
            equivalentAmount.onFailure {
                return null
            }.onSuccess {
                this.equivalentAmount = if (isIncome) it else it?.negate()
            }
        }
    }

    override fun updateAccount(account: Account, isInitialSetup: Boolean) {
        super.updateAccount(account, isInitialSetup)
        if (!isSplitPart) {
            host.loadMethods(account)
        }
        handleDebts()
    }

    override fun createAdapters(withTypeSpinner: Boolean, withAutoFill: Boolean) {
        viewBinding.Payee.createPayeeAdapter(
            withInputMethodNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && configurator[AUTO_COMPLETE_DROPDOWN_SET_INPUT_METHOD_NEEDED, true]
        ) { partyId ->
            handleDebts()
            if (partyId != null) {
                if (withAutoFill && shouldAutoFill) {
                    if (shouldStartAutoFill(prefHandler)) {
                        host.startAutoFill(partyId, false)
                    } else if (!prefHandler.getBoolean(AUTO_FILL_HINT_SHOWN, false)) {
                        ConfirmationDialogFragment.newInstance(Bundle().apply {
                            putLong(KEY_ROWID, partyId)
                            putInt(KEY_TITLE, R.string.information)
                            putString(
                                KEY_MESSAGE,
                                context.getString(R.string.hint_auto_fill)
                            )
                            putInt(KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND)
                            putInt(KEY_COMMAND_NEGATIVE, R.id.AUTO_FILL_COMMAND)
                            putString(
                                KEY_PREFKEY,
                                prefHandler.getKey(AUTO_FILL_HINT_SHOWN)
                            )
                            putInt(KEY_POSITIVE_BUTTON_LABEL, R.string.response_yes)
                            putInt(KEY_NEGATIVE_BUTTON_LABEL, R.string.response_no)
                        }).show(
                            (context as FragmentActivity).supportFragmentManager,
                            "AUTO_FILL_HINT"
                        )
                    }
                }
            }
        }
        super.createAdapters(withTypeSpinner, withAutoFill)
    }

    override fun populateFields(transaction: T, withAutoFill: Boolean) {
        if (!isSplitPart) {
            viewBinding.Payee.party = transaction.party
        }
        transaction.equivalentAmount?.let {
            viewBinding.EquivalentAmount.setFractionDigits(it.currencyUnit.fractionDigits)
            viewBinding.EquivalentAmount.post {
                viewBinding.EquivalentAmount.setAmount(it.amountMajor.abs())
            }
        }
        transaction.originalAmount?.let {
            originalAmountVisible = true
            configureOriginalAmountVisibility()
            viewBinding.OriginalAmount.setFractionDigits(it.currencyUnit.fractionDigits)
            viewBinding.OriginalAmount.setAmount(it.amountMajor)
            originalCurrencyCode = it.currencyUnit.code
        } ?: run {
            originalCurrencyCode = prefHandler.getString(PrefKey.LAST_ORIGINAL_CURRENCY, null)
        }
        populateOriginalCurrency()
        super.populateFields(transaction, withAutoFill)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val originalInputSelectedCurrency = viewBinding.OriginalAmount.selectedCurrency
        if (originalInputSelectedCurrency != null) {
            originalCurrencyCode = originalInputSelectedCurrency.code
        }
        super.onSaveInstanceState(outState)
    }

    abstract fun buildMainTransaction(account: Account): T

    override fun onDestroy() {
        viewBinding.Payee.onDestroy()
    }

    fun setDebts(debts: List<DisplayDebt>) {
        this.debts = debts
        handleDebts()
    }

    fun updateUiWithDebt() {
        updateUiWithDebt(debts.find { it.id == debtId })
    }

    private fun updateUiWithDebt(debt: DisplayDebt?) {
        if (debt == null) {
            if (viewBinding.DebtCheckBox.isChecked) {
                viewBinding.DebtCheckBox.isChecked = false
            }
        }
        updateDebtCheckBox(debt)
    }

    private fun updateDebtCheckBox(debt: DisplayDebt?) {
        val installment = debt?.let { calculateInstallment(it) }
        viewBinding.DebtCheckBox.text = debt?.let { formatDebt(it, installment) } ?: ""
        val infoText = installment?.let {
            try {
                formatDebtHelp(debt, it)
            } catch (_: Exception) {
                null
            }
        }
        viewBinding.DebtSummaryPopup.isVisible = if (infoText != null) {
            viewBinding.DebtSummaryPopup.contentDescription = infoText
            viewBinding.DebtSummaryPopup.configurePopupAnchor(infoText)
            true
        } else false

    }

    private fun calculateInstallment(debt: DisplayDebt) =
        if (debt.currency != currentAccount()!!.currency)
            with(
                viewBinding.EquivalentAmount.getAmount(
                    showToUser = false
                ) ?: BigDecimal.ZERO
            ) {
                if (isIncome) this else this.negate()
            }
        else validateAmountInput()

    private fun formatDebtHelp(debt: DisplayDebt, installment: BigDecimal) =
        TextUtils.concat(*buildList {
            val installmentSign = installment.signum()
            val debtSign = debt.currentBalance.sign

            add(
                context.getString(
                    when (installmentSign) {
                        1 -> {
                            when (debtSign) {
                                1 -> R.string.debt_installment_receive
                                else -> R.string.debt_borrow_additional
                            }
                        }

                        -1 -> {
                            when (debtSign) {
                                -1 -> R.string.debt_installment_pay
                                else -> R.string.debt_lend_additional
                            }
                        }

                        else -> throw IllegalStateException()
                    },
                    currencyFormatter.formatMoney(Money(debt.currency, installment.abs()))
                )
            )

            val currentBalance = Money(debt.currency, debt.currentBalance)
            val futureBalance = Money(debt.currency, currentBalance.amountMajor - installment)

            val futureSign = futureBalance.amountMajor.signum()

            if (futureSign != debtSign) {
                when (debtSign) {
                    1 -> context.getString(R.string.debt_paid_off_other, debt.payeeName)
                    -1 -> context.getString(R.string.debt_paid_off_self)
                    else -> null
                }?.let { add(it) }
            }

            val futureBalanceAbs =
                currencyFormatter.formatMoney(Money(debt.currency, futureBalance.amountMajor.abs()))
            when (futureSign) {
                1 -> context.getString(
                    R.string.debt_balance_they_owe,
                    debt.payeeName,
                    futureBalanceAbs
                )

                -1 -> context.getString(R.string.debt_balance_i_owe, futureBalanceAbs)
                else -> null
            }?.let { add(it) }

        }.toTypedArray())

    private fun formatDebt(debt: DisplayDebt, withInstallment: BigDecimal? = null): CharSequence {
        val amount = debt.currentBalance
        val money = Money(debt.currency, amount)
        val elements = buildList {
            add(debt.label)
            add(" ")
            add(
                currencyFormatter.formatMoney(money)
                    .withAmountColor(viewBinding.root.context.resources, amount.sign)
            )
            withInstallment?.let {
                add(" ${Transfer.RIGHT_ARROW} ")
                val futureBalance = money.amountMajor - it
                add(
                    try {
                        currencyFormatter.formatMoney(Money(debt.currency, futureBalance))
                    } catch (e: ArithmeticException) {
                        e.safeMessage
                    }
                        .withAmountColor(
                            viewBinding.root.context.resources,
                            futureBalance.signum()
                        )
                )
            }
        }

        return TextUtils.concat(*elements.toTypedArray())
    }

    private fun setDebt(debt: DisplayDebt) {
        updateUiWithDebt(debt)
        debtId = debt.id
        host.setDirty()
        val party = viewBinding.Payee.party
        if (party == null) {
            val focussed: Boolean = viewBinding.Payee.hasFocus()
            if (focussed) {
                viewBinding.Payee.clearFocus()
            }
            viewBinding.Payee.party = DisplayParty(debt.payeeId, debt.payeeName)
            if (focussed) {
                viewBinding.Amount.requestFocus()
            }
        }
    }

    private val applicableDebts: List<DisplayDebt>
        get() = debts.filter { it.currency == currentAccount()?.currency || it.currency == homeCurrency }

    private fun handleDebts() {
        applicableDebts.let { debts ->
            val hasDebts = debts.isNotEmpty()
            viewBinding.DebtRow.visibility = if (hasDebts) View.VISIBLE else View.GONE
            if (hasDebts) {
                if (debtId != null) {
                    updateUiWithDebt()
                    if (!viewBinding.DebtCheckBox.isChecked) {
                        viewBinding.DebtCheckBox.isChecked = true
                    }
                } else {
                    updateUiWithDebt(singleDebtForPayee(debts))
                }
            } else {
                updateUiWithDebt(null)
            }
        }
    }

    private fun singleDebtForPayee(debts: List<DisplayDebt>) =
        if (debts.size == 1) debts.first().takeIf { it.payeeId == payeeId } else null

    fun setupDebtChangedListener() {
        viewBinding.DebtCheckBox.setOnCheckedChangeListener { _, isChecked ->
            applicableDebts.let { debts ->
                if (isChecked && !host.isFinishing) {
                    when (debts.size) {
                        0 -> { /*should not happen*/
                            CrashHandler.throwOrReport(
                                "Debt checked without applicable debt"
                            )
                        }

                        else -> {
                            singleDebtForPayee(debts)?.also {
                                setDebt(it)
                            } ?: run {
                                val sortedDebts =
                                    debts.sortedWith(compareBy<DisplayDebt> { it.payeeId != payeeId }.thenBy { it.payeeId })
                                with(PopupMenu(context, viewBinding.DebtCheckBox)) {
                                    var currentMenu: Menu? = null
                                    var currentPayee: Long? = null
                                    sortedDebts.forEachIndexed { index, debt ->
                                        if (debt.payeeId != currentPayee) {
                                            currentPayee = debt.payeeId
                                            val subMenuTitle = if (debt.payeeId == payeeId)
                                                SpannableStringBuilder().bold { append(debt.payeeName) }
                                            else
                                                debt.payeeName
                                            currentMenu = menu.addSubMenu(
                                                Menu.NONE,
                                                -1,
                                                Menu.NONE,
                                                subMenuTitle
                                            )
                                        }
                                        currentMenu!!.add(
                                            Menu.NONE,
                                            index,
                                            Menu.NONE,
                                            formatDebt(debt)
                                        )
                                    }
                                    var subMenuOpen = false
                                    setOnMenuItemClickListener { item ->
                                        when (item.itemId) {
                                            -1 -> subMenuOpen = true
                                            else -> setDebt(sortedDebts[item.itemId])
                                        }
                                        true
                                    }
                                    setOnDismissListener {
                                        if (subMenuOpen) {
                                            subMenuOpen = false
                                        } else if (debtId == null) {
                                            viewBinding.DebtCheckBox.isChecked = false
                                        }
                                    }
                                    show()
                                }
                            }
                        }
                    }
                } else {
                    updateUiWithDebt(null)
                    debtId = null
                }
            }
        }
    }

    override fun configureAccountDependent(account: Account, isInitialSetup: Boolean) {
        super.configureAccountDependent(account, isInitialSetup)
        val currencyUnit = account.currency
        viewBinding.OriginalAmount.configureExchange(currencyUnit)
        val needsEquivalentAmount = !isSplitPart && !isTemplate &&
                !hasHomeCurrency(account) &&
                account.isDynamic
        viewBinding.EquivalentAmountRow.isVisible = needsEquivalentAmount
        if (needsEquivalentAmount) {
            viewBinding.EquivalentAmount.configureExchange(currencyUnit, homeCurrency)
            if (isInitialSetup) {
                loadPrice()
            }
        }
    }

    fun loadPrice() {
        if (viewBinding.EquivalentAmountRow.isVisible) {
            viewBinding.EquivalentAmount.loadExchangeRate()
        }
    }
}