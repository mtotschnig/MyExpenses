package org.totschnig.myexpenses.delegate

import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.FilterQueryProvider
import android.widget.SimpleCursorAdapter
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
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
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_HINT_SHOWN
import org.totschnig.myexpenses.preference.shouldStartAutoFill
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.configurePopupAnchor
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.math.BigDecimal
import kotlin.math.sign

//Transaction or Split
abstract class MainDelegate<T : ITransaction>(
    viewBinding: OneExpenseBinding,
    dateEditBinding: DateEditBinding,
    methodRowBinding: MethodRowBinding,
    isTemplate: Boolean
) : TransactionDelegate<T>(
    viewBinding,
    dateEditBinding,
    methodRowBinding,
    isTemplate
) {
    private var debts: List<Debt> = emptyList()
    private lateinit var payeeAdapter: SimpleCursorAdapter


    override fun bind(
        transaction: T?,
        withTypeSpinner: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean
    ) {
        super.bind(
            transaction,
            withTypeSpinner,
            savedInstanceState,
            recurrence,
            withAutoFill
        )
        val textWatcher = object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                onAmountChanged()
            }
        }
        viewBinding.Amount.addTextChangedListener(textWatcher)
        viewBinding.EquivalentAmount.addTextChangedListener(textWatcher)
        payeeId = host.parentPayeeId
    }

    fun onAmountChanged() {
        if (debtId != null) {
            updateDebtCheckBox(debts.find { it.id == debtId })
        }
    }

    override fun buildTransaction(
        forSave: Boolean,
        account: Account
    ): T? {
        val amount = validateAmountInput(forSave, currentAccount()!!.currency).getOrNull()
            ?: //Snackbar is shown in validateAmountInput
            return null
        return buildMainTransaction(account).apply {
            this.amount = amount
            if (this@MainDelegate.payeeId != null) {
                this.payeeId = this@MainDelegate.payeeId
            } else {
                payee = viewBinding.Payee.text.toString()
            }
            this.debtId = this@MainDelegate.debtId
            this.methodId = this@MainDelegate.methodId
            val selectedItem = viewBinding.OriginalAmount.selectedCurrency
            if (selectedItem != null) {
                val currency = selectedItem.code
                val originalAmount = validateAmountInput(
                    viewBinding.OriginalAmount,
                    showToUser = true,
                    ifPresent = true,
                    currencyUnit = currencyContext[currency]
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
            val equivalentAmount = validateAmountInput(
                viewBinding.EquivalentAmount,
                showToUser = true,
                ifPresent = true,
                homeCurrency
            )
            equivalentAmount.onFailure {
                return null
            }.onSuccess {
                this.equivalentAmount = if (isIncome) it else it?.negate()
            }
        }
    }

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        if (!isSplitPart) {
            host.loadMethods(account)
        }
        handleDebts()
    }

    override fun createAdapters(withTypeSpinner: Boolean, withAutoFill: Boolean) {
        createPayeeAdapter(withAutoFill)
        createStatusAdapter()
        if (withTypeSpinner) {
            createOperationTypeAdapter()
        }
    }

    override fun populateFields(transaction: T, withAutoFill: Boolean) {
        super.populateFields(transaction, withAutoFill)
        if (!isSplitPart)
            viewBinding.Payee.setText(transaction.payee)
    }

    abstract fun buildMainTransaction(account: Account): T

    private fun createPayeeAdapter(withAutoFill: Boolean) {
        payeeAdapter = SimpleCursorAdapter(
            context,
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            null,
            arrayOf(KEY_PAYEE_NAME),
            intArrayOf(android.R.id.text1),
            0
        )
        viewBinding.Payee.setAdapter(payeeAdapter)
        payeeAdapter.filterQueryProvider = FilterQueryProvider { constraint: CharSequence? ->
            if (constraint != null) {
                val (selection, selectArgs) =
                    " AND ${Party.SELECTION}" to Party.selectionArgs(
                        Utils.escapeSqlLikeExpression(Utils.normalize(constraint.toString()))
                    )
                context.contentResolver.query(
                    TransactionProvider.PAYEES_URI,
                    arrayOf(KEY_ROWID, KEY_PAYEE_NAME),
                    "$KEY_PARENTID IS NULL $selection",
                    selectArgs,
                    null
                )
            } else null
        }
        payeeAdapter.stringConversionColumn = 1
        viewBinding.Payee.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val c = payeeAdapter.getItem(position) as Cursor
                if (c.moveToPosition(position)) {
                    c.getLong(0).let {
                        payeeId = it
                        handleDebts()
                        if (withAutoFill && shouldAutoFill) {
                            if (shouldStartAutoFill(prefHandler)) {
                                host.startAutoFill(it, false)
                            } else if (!prefHandler.getBoolean(AUTO_FILL_HINT_SHOWN, false)) {
                                ConfirmationDialogFragment.newInstance(Bundle().apply {
                                    putLong(KEY_ROWID, it)
                                    putInt(KEY_TITLE, R.string.dialog_title_information)
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
            }
    }

    override fun onDestroy() {
        if (::payeeAdapter.isInitialized) {
            payeeAdapter.cursor?.let {
                if (!it.isClosed) {
                    it.close()
                }
            }
        } else {
            CrashHandler.report(IllegalStateException("PayeeAdapter not initialized"))
        }
    }

    fun setDebts(debts: List<Debt>) {
        this.debts = debts
        handleDebts()
    }

    fun updateUiWithDebt() {
        updateUiWithDebt(debts.find { it.id == debtId })
    }

    private fun updateUiWithDebt(debt: Debt?) {
        if (debt == null) {
            if (viewBinding.DebtCheckBox.isChecked) {
                viewBinding.DebtCheckBox.isChecked = false
            }
        }
        updateDebtCheckBox(debt)
    }

    private fun updateDebtCheckBox(debt: Debt?) {
        val installment = debt?.let { calculateInstallment(it) }
        viewBinding.DebtCheckBox.text = debt?.let { formatDebt(it, installment) } ?: ""
        viewBinding.DebtSummaryPopup.isVisible = installment != null
        installment?.let {
            val infoText = formatDebtHelp(debt, it)
            viewBinding.DebtSummaryPopup.contentDescription = infoText
            viewBinding.DebtSummaryPopup.configurePopupAnchor(infoText)
        }
    }

    private fun calculateInstallment(debt: Debt) =
        (if (debt.currency != currentAccount()!!.currency)
            with(
                validateAmountInput(
                    viewBinding.EquivalentAmount,
                    showToUser = false,
                    ifPresent = false
                )
            ) {
                if (isIncome) this else this?.negate()
            }
        else
            validateAmountInput()).takeIf { it != BigDecimal.ZERO }

    private fun formatDebtHelp(debt: Debt, installment: BigDecimal) =
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

    private fun formatDebt(debt: Debt, withInstallment: BigDecimal? = null): CharSequence {
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
                    currencyFormatter.formatMoney(Money(debt.currency, futureBalance))
                        .withAmountColor(
                            viewBinding.root.context.resources,
                            futureBalance.signum()
                        )
                )
            }
        }

        return TextUtils.concat(*elements.toTypedArray())
    }

    private fun setDebt(debt: Debt) {
        updateUiWithDebt(debt)
        debtId = debt.id
        host.setDirty()
        if (debt.currency != currentAccount()!!.currency) {
            if (!equivalentAmountVisible) {
                equivalentAmountVisible = true
                configureEquivalentAmount()
            }
        }
        if (viewBinding.Payee.text.isEmpty()) {
            val focussed: Boolean = viewBinding.Payee.hasFocus()
            if (focussed) {
                viewBinding.Payee.clearFocus()
            }
            viewBinding.Payee.setText(debt.payeeName)
            if (focussed) {
                viewBinding.Amount.requestFocus()
            }
            payeeId = debt.payeeId
        }
    }

    private val applicableDebts: List<Debt>
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

    private fun singleDebtForPayee(debts: List<Debt>) =
        if (debts.size == 1) debts.first().takeIf { it.payeeId == payeeId } else null

    override fun setupListeners(watcher: TextWatcher) {
        super.setupListeners(watcher)
        viewBinding.Payee.addTextChangedListener {
            if (viewBinding.Payee.isFocused) {
                payeeId = null
                handleDebts()
            }
        }
    }

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
                                    debts.sortedWith(compareBy<Debt> { it.payeeId != payeeId }.thenBy { it.payeeId })
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
}