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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.*
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_HINT_SHOWN
import org.totschnig.myexpenses.preference.shouldStartAutoFill
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.MyTextWatcher
import org.totschnig.myexpenses.util.TextUtils.withAmountColor
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.Debt
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
        newInstance: Boolean,
        savedInstanceState: Bundle?,
        recurrence: Plan.Recurrence?,
        withAutoFill: Boolean
    ) {
        super.bind(
            transaction,
            newInstance,
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
        accountId: Long
    ): T? {
        val amount = validateAmountInput(forSave, currentAccount()!!.currency).getOrNull()
            ?: //Snackbar is shown in validateAmountInput
            return null
        return buildMainTransaction(accountId).apply {
            this.amount = amount
            payee = viewBinding.Payee.text.toString()
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
                Utils.getHomeCurrency()
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

    override fun createAdapters(newInstance: Boolean, withAutoFill: Boolean) {
        createPayeeAdapter(withAutoFill)
        createStatusAdapter()
        if (newInstance) {
            createOperationTypeAdapter()
        }
    }

    override fun populateFields(transaction: T, withAutoFill: Boolean) {
        super.populateFields(transaction, withAutoFill)
        if (!isSplitPart)
            viewBinding.Payee.setText(transaction.payee)
    }

    abstract fun buildMainTransaction(accountId: Long): T

    private fun createPayeeAdapter(withAutoFill: Boolean) {
        payeeAdapter = SimpleCursorAdapter(
            context,
            R.layout.support_simple_spinner_dropdown_item,
            null,
            arrayOf(KEY_PAYEE_NAME),
            intArrayOf(android.R.id.text1),
            0
        )
        viewBinding.Payee.setAdapter(payeeAdapter)
        payeeAdapter.filterQueryProvider = FilterQueryProvider { constraint: CharSequence? ->
            var selection: String? = null
            var selectArgs = arrayOfNulls<String>(0)
            if (constraint != null) {
                selection = Payee.SELECTION
                selectArgs =
                    Payee.SELECTION_ARGS(Utils.escapeSqlLikeExpression(Utils.normalize(constraint.toString())))
            }
            context.contentResolver.query(
                TransactionProvider.PAYEES_URI,
                arrayOf(KEY_ROWID, KEY_PAYEE_NAME),
                selection,
                selectArgs,
                null
            )
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
                                newInstance(Bundle().apply {
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

    private fun updateUiWithDebt(debt: Debt?) {
        if (debt == null) {
            if (viewBinding.DebtCheckBox.isChecked) {
                viewBinding.DebtCheckBox.isChecked = false
            }
        }
        updateDebtCheckBox(debt)
    }

    private fun updateDebtCheckBox(debt: Debt?) {
        viewBinding.DebtCheckBox.text = debt?.let { formatDebt(it, true) } ?: ""
    }

    private fun formatDebt(debt: Debt, withInstallment: Boolean = false): CharSequence {
        val amount = debt.currentBalance
        val money = Money(debt.currency, amount)
        val elements = mutableListOf<CharSequence>().apply {
            add(debt.label)
            add(" ")
            add(
                currencyFormatter.formatMoney(money)
                    .withAmountColor(viewBinding.root.context.resources, amount.sign)
            )
        }
        val account = currentAccount()
        if (withInstallment && account != null) {
            val isForeignExchangeDebt = debt.currency != account.currency

            val installment = if (isForeignExchangeDebt)
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
                validateAmountInput()

            if (installment != null) {
                elements.add(" ${Transfer.RIGHT_ARROW} ")
                val futureBalance = money.amountMajor - installment
                elements.add(
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
    }

    private val applicableDebts: List<Debt>
        get() = debts.filter { it.currency == currentAccount()?.currency || it.currency == Utils.getHomeCurrency() }

    private fun handleDebts() {
        applicableDebts.let { debts ->
            val hasDebts = debts.isNotEmpty()
            viewBinding.DebtRow.visibility = if (hasDebts) View.VISIBLE else View.GONE
            if (hasDebts) {
                if (debtId != null) {
                    updateUiWithDebt(debts.find { it.id == debtId })
                    if (!viewBinding.DebtCheckBox.isChecked) {
                        viewBinding.DebtCheckBox.isChecked = true
                    }
                } else if (isSingleDebtForPayee(debts)) {
                    updateUiWithDebt(debts.first())
                }
            } else {
                updateUiWithDebt(null)
            }
        }
    }

    private fun isSingleDebtForPayee(debts: List<Debt>) =
        debts.size == 1 && debts.first().payeeId == payeeId

    override fun setupListeners(watcher: TextWatcher) {
        super.setupListeners(watcher)
        viewBinding.Payee.addTextChangedListener {
            payeeId = null
            handleDebts()
        }
    }

    fun setupDebtChangedListener() {
        viewBinding.DebtCheckBox.setOnCheckedChangeListener { _, isChecked ->
            applicableDebts.let { debts ->
                if (isChecked && !host.isFinishing) {
                    when (debts.size) {
                        0 -> { /*should not happen*/ CrashHandler.throwOrReport(
                            "Debt checked without applicable debt"
                        )
                        }
                        else -> {
                            if (isSingleDebtForPayee(debts)) {
                                setDebt(debts.first())
                            } else {
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
                    if (debts.size > 1) {
                        updateUiWithDebt(null)
                    }
                    debtId = null
                }
            }
        }
    }
}