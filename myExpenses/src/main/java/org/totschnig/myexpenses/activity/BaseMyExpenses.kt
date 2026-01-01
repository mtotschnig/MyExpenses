package org.totschnig.myexpenses.activity

import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses.Companion.MANAGE_HIDDEN_FRAGMENT_TAG
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.provider.KEY_COLOR
import org.totschnig.myexpenses.provider.KEY_ROWID
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
}