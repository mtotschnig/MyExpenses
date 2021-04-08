package org.totschnig.myexpenses.delegate

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.FilterQueryProvider
import android.widget.SimpleCursorAdapter
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.DateEditBinding
import org.totschnig.myexpenses.databinding.MethodRowBinding
import org.totschnig.myexpenses.databinding.OneExpenseBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Payee
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.shouldStartAutoFill
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Account

//Transaction or Split
abstract class MainDelegate<T : ITransaction>(viewBinding: OneExpenseBinding, dateEditBinding: DateEditBinding, methodRowBinding: MethodRowBinding, prefHandler: PrefHandler, isTemplate: Boolean) : TransactionDelegate<T>(viewBinding, dateEditBinding, methodRowBinding, prefHandler, isTemplate) {
    private lateinit var payeeAdapter: SimpleCursorAdapter

    override fun buildTransaction(forSave: Boolean, currencyContext: CurrencyContext, accountId: Long): T? {
        val amount = validateAmountInput(forSave)
                ?: //Snackbar is shown in validateAmountInput
                return null
        return buildMainTransaction(accountId).apply {
            this.amount = Money(currentAccount()!!.currency, amount)
            payee = viewBinding.Payee.text.toString()
            this.methodId = this@MainDelegate.methodId
            val originalAmount = validateAmountInput(viewBinding.OriginalAmount, showToUser = false, ifPresent = true)
            val selectedItem = viewBinding.OriginalAmount.selectedCurrency
            if (selectedItem != null && originalAmount != null) {
                val currency = selectedItem.code
                PrefKey.LAST_ORIGINAL_CURRENCY.putString(currency)
                this.originalAmount = Money(currencyContext[currency], originalAmount)
            } else {
                this.originalAmount = null
            }
            val equivalentAmount = validateAmountInput(viewBinding.EquivalentAmount, showToUser = false, ifPresent = true)
            this.equivalentAmount = if (equivalentAmount == null) null else Money(Utils.getHomeCurrency(), if (isIncome) equivalentAmount else equivalentAmount.negate())
        }
    }

    override fun updateAccount(account: Account) {
        super.updateAccount(account)
        if (!isSplitPart) {
            host.loadMethods(account)
        }
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
        payeeAdapter = SimpleCursorAdapter(context, R.layout.support_simple_spinner_dropdown_item, null, arrayOf(DatabaseConstants.KEY_PAYEE_NAME), intArrayOf(android.R.id.text1),
                0)
        viewBinding.Payee.setAdapter(payeeAdapter)
        payeeAdapter.filterQueryProvider = FilterQueryProvider { constraint: CharSequence? ->
            var selection: String? = null
            var selectArgs = arrayOfNulls<String>(0)
            if (constraint != null) {
                selection = Payee.SELECTION
                selectArgs = Payee.SELECTION_ARGS(Utils.escapeSqlLikeExpression(Utils.normalize(constraint.toString())))
            }
            context.contentResolver.query(
                    TransactionProvider.PAYEES_URI, arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                    selection, selectArgs, null)
        }
        payeeAdapter.stringConversionColumn = 1
        viewBinding.Payee.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            val c = payeeAdapter.getItem(position) as Cursor
            if (c.moveToPosition(position)) {
                c.getLong(0).let {
                    if (withAutoFill && shouldAutoFill) {
                        if (prefHandler.getBoolean(PrefKey.AUTO_FILL_HINT_SHOWN, false)) {
                            if (shouldStartAutoFill(prefHandler)) {
                                host.startAutoFill(it, false)
                            }
                        } else {
                            val b = Bundle()
                            b.putLong(DatabaseConstants.KEY_ROWID, it)
                            b.putInt(ConfirmationDialogFragment.KEY_TITLE, R.string.dialog_title_information)
                            b.putString(ConfirmationDialogFragment.KEY_MESSAGE, context.getString(R.string.hint_auto_fill))
                            b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.AUTO_FILL_COMMAND)
                            b.putInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE, R.id.AUTO_FILL_COMMAND)
                            b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
                                    prefHandler.getKey(PrefKey.AUTO_FILL_HINT_SHOWN))
                            b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.response_yes)
                            b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.response_no)
                            ConfirmationDialogFragment.newInstance(b).show((context as FragmentActivity).supportFragmentManager,
                                    "AUTO_FILL_HINT")
                        }
                    }
                }

            }
        }
    }

    override fun onDestroy() {
        payeeAdapter.cursor?.let {
            if (!it.isClosed) {
                it.close()
            }
        }
    }
}