package org.totschnig.myexpenses.activity

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses.Companion.MANAGE_HIDDEN_FRAGMENT_TAG
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.provider.CheckSealedHandler
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount

abstract class BaseMyExpenses<T : MyExpensesViewModel> : LaunchActivity() {

    lateinit var viewModel: T

    protected fun editAccount(account: FullAccount) {
        startActivity(Intent(this, AccountEdit::class.java).apply {
            putExtra(KEY_ROWID, account.id)
            putExtra(KEY_COLOR, account.color)
        })
    }

    protected fun confirmAccountDelete(account: FullAccount) {
        MessageDialogFragment.newInstance(
            resources.getQuantityString(
                R.plurals.dialog_title_warning_delete_account,
                1,
                1
            ),
            getString(
                R.string.warning_delete_account,
                account.label
            ) + " " + getString(R.string.continue_confirmation),
            MessageDialogFragment.Button(
                R.string.menu_delete,
                R.id.DELETE_ACCOUNT_COMMAND_DO,
                longArrayOf(account.id)
            ),
            null,
            MessageDialogFragment.noButton(), 0
        )
            .show(supportFragmentManager, "DELETE_ACCOUNT")
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        } else when (command) {
            R.id.DELETE_ACCOUNT_COMMAND_DO -> {
                val accountIds = tag as LongArray
                val manageHiddenFragment =
                    supportFragmentManager.findFragmentByTag(MANAGE_HIDDEN_FRAGMENT_TAG)
                if (manageHiddenFragment != null) {
                    supportFragmentManager.beginTransaction().remove(manageHiddenFragment).commit()
                }
                showSnackBarIndefinite(R.string.progress_dialog_deleting)
                viewModel.deleteAccounts(accountIds).observe(this) { result ->
                    result.onSuccess {
                        showSnackBar(
                            resources.getQuantityString(
                                R.plurals.delete_success,
                                accountIds.size,
                                accountIds.size
                            )
                        )
                    }.onFailure {
                        if (it is AccountSealedException) {
                            showSnackBar(R.string.object_sealed_debt)
                        } else {
                            showDeleteFailureFeedback(null)
                        }
                    }
                }
            }
            else -> return false
        }
        return true
    }

    protected fun toggleAccountSealed(account: FullAccount, snackBarContainer: View? = null) {
        if (account.sealed) {
            viewModel.setSealed(account.id, false)
        } else {
            if (account.syncAccountName == null) {
                viewModel.setSealed(account.id, true)
            } else {
                showSnackBar(
                    getString(R.string.warning_synced_account_cannot_be_closed),
                    Snackbar.LENGTH_LONG, null, null, snackBarContainer
                )
            }
        }
    }

    protected fun toggleExcludeFromTotals(account: FullAccount) {
        viewModel.setExcludeFromTotals(account.id, !account.excludeFromTotals)
    }

    protected fun toggleDynamicExchangeRate(account: FullAccount) {
        viewModel.setDynamicExchangeRate(account.id, !account.dynamic)
    }

    var selectedAccountId: Long
        get() = viewModel.selectedAccountId
        set(value) {
            viewModel.selectAccount(value)
        }

    protected val createAccount =
        registerForActivityResult(AccountEdit.Companion.CreateContract()) {
            if (it != null) {
                selectedAccountId = it
            }
        }

    fun createRow(
        @Transactions.TransactionType type: Int,
        transferEnabled: Boolean,
        isIncome: Boolean = false,
    ) {
        when (type) {
            TYPE_SPLIT -> contribFeatureRequested(ContribFeature.SPLIT_TRANSACTION)
            TYPE_TRANSFER if !transferEnabled -> showTransferAccountMissingMessage()
            else -> createRowDo(type, isIncome)
        }
    }

    fun createRowDo(type: Int, isIncome: Boolean) {
        lifecycleScope.launch {
            createRowIntent(type, isIncome)?.let { startEdit(it) }
        }
    }

    /**
     * start ExpenseEdit Activity for a new transaction/transfer/split
     * Originally the form for transaction is rendered, user can change from spinner in toolbar
     */
    suspend fun createRowIntent(type: Int, isIncome: Boolean) = getEditIntent()?.apply {
        putExtra(Transactions.OPERATION_TYPE, type)
        putExtra(ExpenseEdit.KEY_INCOME, isIncome)
    }

    protected fun toggleCrStatus(transactionId: Long) {
        checkSealed(listOf(transactionId), withTransfer = false) {
            viewModel.toggleCrStatus(transactionId)
        }
    }

    open val checkSealedHandler by lazy { CheckSealedHandler(contentResolver) }

    fun checkSealed(itemIds: List<Long>, withTransfer: Boolean = true, onChecked: Runnable) {
        checkSealedHandler.check(itemIds, withTransfer) { result ->
            lifecycleScope.launchWhenResumed {
                result.onSuccess {
                    if (it.first && it.second) {
                        onChecked.run()
                    } else {
                        warnSealedAccount(!it.first, !it.second, itemIds.size > 1)
                    }
                }.onFailure {
                    showSnackBar(it.safeMessage)
                }
            }
        }
    }

    private fun warnSealedAccount(sealedAccount: Boolean, sealedDebt: Boolean, multiple: Boolean) {
        val resIds = mutableListOf<Int>()
        if (multiple) {
            resIds.add(R.string.warning_account_for_transaction_is_closed)
        }
        if (sealedAccount) {
            resIds.add(R.string.object_sealed)
        }
        if (sealedDebt) {
            resIds.add(R.string.object_sealed_debt)
        }
        showSnackBar(TextUtils.concatResStrings(this, *resIds.toIntArray()))
    }
}