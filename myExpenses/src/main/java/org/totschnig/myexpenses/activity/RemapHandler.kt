package org.totschnig.myexpenses.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.lifecycleScope
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity.Companion.PROGRESS_TAG
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_CHECKBOX_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_POSITIVE_BUTTON_CHECKED_LABEL
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_TITLE_STRING
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectSingleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectSingleMethodDialogFragment
import org.totschnig.myexpenses.provider.CheckTransferAccountOfSplitPartsHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.util.safeMessage

class RemapHandler(val activity: BaseMyExpenses) : FragmentResultListener {

    init {
        for (requestKey in listOf(MAP_ACCOUNT_REQUEST, MAP_METHOD_REQUEST)) {
            activity.supportFragmentManager.setFragmentResultListener(requestKey, activity, this)
        }
    }

    private val getPayee =
        activity.registerForActivityResult(PickObjectContract(MAP_PAYEE_REQUEST)) {}
    private val getCategory =
        activity.registerForActivityResult(PickObjectContract(MAP_CATEGORY_REQUEST)) {}

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any?) =
        activity.getString(resId, *formatArgs)

    fun parseResult(result: Bundle) =
        result.getLong(KEY_ROWID) to result.getString(DatabaseConstants.KEY_LABEL)!!

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        onResult(requestKey, parseResult(result))
    }

    fun onResult(requestKey: String, result: Pair<Long, String>) {
        val (rowId, label) = result
        val columnStringResId: Int
        val confirmationStringResId: Int
        val column: String
        when (requestKey) {
            MAP_CATEGORY_REQUEST -> {
                column = DatabaseConstants.KEY_CATID
                columnStringResId = R.string.category
                confirmationStringResId = R.string.remap_category
            }
            MAP_PAYEE_REQUEST -> {
                column = DatabaseConstants.KEY_PAYEEID
                columnStringResId = R.string.payer_or_payee
                confirmationStringResId = R.string.remap_payee
            }
            MAP_METHOD_REQUEST -> {
                column = DatabaseConstants.KEY_METHODID
                columnStringResId = R.string.method
                confirmationStringResId = R.string.remap_method
            }
            MAP_ACCOUNT_REQUEST -> {
                column = DatabaseConstants.KEY_ACCOUNTID
                columnStringResId = R.string.account
                confirmationStringResId = R.string.remap_account
            }
            else -> throw IllegalStateException("Unexpected value: $requestKey")
        }
        activity.showConfirmationDialog("dialogRemap",
            getString(confirmationStringResId, label) + " " + getString(R.string.continue_confirmation),
            R.id.REMAP_COMMAND, R.string.menu_remap,
            commandNegative = null
        ) {
            putString(KEY_COLUMN, column)
            putLong(KEY_ROWID, rowId)
            putString(
                KEY_TITLE_STRING,
                getString(R.string.dialog_title_confirm_remap, getString(columnStringResId))
            )
            putInt(KEY_POSITIVE_BUTTON_CHECKED_LABEL, R.string.button_label_clone_and_remap)
            putString(KEY_CHECKBOX_LABEL, getString(R.string.menu_clone_transaction))
        }
    }

    fun remap(extras: Bundle, shouldClone: Boolean) {
        with(activity) {
            val checkedItemIds = selectionState.map { it.id }
            val column = extras.getString(KEY_COLUMN) ?: return
            if (shouldClone) {
                val progressDialog = ProgressDialogFragment.newInstance(
                    getString(R.string.saving), null, ProgressDialog.STYLE_HORIZONTAL, false
                )
                progressDialog.max = checkedItemIds.size
                supportFragmentManager
                    .beginTransaction()
                    .add(progressDialog, PROGRESS_TAG)
                    .commit()
                viewModel.cloneAndRemap(
                    checkedItemIds,
                    column,
                    extras.getLong(KEY_ROWID)
                )
            } else {
                viewModel.remap(checkedItemIds, column, extras.getLong(KEY_ROWID))
                    .observe(this) { result: Int ->
                        val message =
                            if (result > 0) getString(R.string.remapping_result) else "No transactions were mapped"
                        showSnackBar(message)
                    }
            }
        }
    }

    private inner class PickObjectContract(private val requestKey: String) :
        ActivityResultContract<Unit, Unit>() {
        override fun createIntent(context: Context, input: Unit) =
            Intent(
                context, when (requestKey) {
                    MAP_CATEGORY_REQUEST -> ManageCategories::class.java
                    MAP_PAYEE_REQUEST -> ManageParties::class.java
                    else -> throw java.lang.IllegalArgumentException()
                }
            ).apply {
                action = Action.SELECT_MAPPING.name
            }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            if (resultCode == Activity.RESULT_OK) {
                intent?.extras?.let { onResult(requestKey, it) }
            }
        }
    }

    private fun onResult(requestKey: String, result: Bundle) {
        val rowId = result.getLong(KEY_ROWID)
        val label = result.getString(DatabaseConstants.KEY_LABEL)
        if (rowId != 0L && label != null) {
            onResult(requestKey, rowId to label)
        }
    }

    private fun remapAccount() {
        with(activity) {
            val itemIds = selectionState.map { it.id }
            checkSealed(itemIds) {
                val transferAccountIds = selectionState.mapNotNull { it.transferAccount }
                val excludedIds = if (currentAccount!!.id > 0) transferAccountIds + currentAccount!!.id
                else transferAccountIds
                val splitIds = selectionState.filter { it.isSplit }.map { it.id }
                CheckTransferAccountOfSplitPartsHandler(contentResolver).check(splitIds) { result ->
                    lifecycleScope.launchWhenResumed {
                        result.onSuccess {
                            SelectSingleAccountDialogFragment.newInstance(
                                R.string.menu_remap,
                                R.string.remap_empty_list,
                                excludedIds + it
                            ).show(supportFragmentManager, "REMAP_ACCOUNT")
                        }.onFailure {
                            showSnackBar(it.safeMessage)
                        }
                    }
                }
            }
        }
    }

    private fun remapMethod() {
        with(activity) {
            val itemIds = selectionState.map { it.id }
            checkSealed(itemIds) {
                val hasExpense = selectionState.any { it.amount.amountMinor < 0 }
                val hasIncome = selectionState.any { it.amount.amountMinor > 0 }
                val accountTypes = if (currentAccount!!.isAggregate)
                    selectionState.mapNotNull { it.accountType?.name }.distinct().toTypedArray()
                else arrayOf(currentAccount!!.type!!.name)
                val type = when {
                    hasExpense && !hasIncome -> -1
                    hasIncome && !hasExpense -> 1
                    else -> 0
                }
                lifecycleScope.launchWhenResumed {
                    val dialogFragment = SelectSingleMethodDialogFragment.newInstance(
                        R.string.menu_remap,
                        R.string.remap_empty_list,
                        accountTypes,
                        type
                    )
                    dialogFragment.show(supportFragmentManager, "REMAP_METHOD")
                }
            }
        }
    }

    private fun remapPayee() {
        with(activity) {
            checkSealed(selectionState.map { it.id }) {
                getPayee.launch(Unit)
            }
        }
    }

    private fun remapCategory() {
        with(activity) {
            checkSealed(selectionState.map { it.id }) {
                getCategory.launch(Unit)
            }
        }
    }

    fun handleActionItemClick(itemId: Int): Boolean {
        when (itemId) {
            R.id.REMAP_ACCOUNT_COMMAND -> remapAccount()
            R.id.REMAP_METHOD_COMMAND -> remapMethod()
            R.id.REMAP_PAYEE_COMMAND -> remapPayee()
            R.id.REMAP_CATEGORY_COMMAND -> remapCategory()
            else -> return false
        }
        return true
    }

    companion object {
        const val KEY_COLUMN = "column"
        const val MAP_CATEGORY_REQUEST = "mapCategory"
        const val MAP_PAYEE_REQUEST = "mapPayee"
        const val MAP_METHOD_REQUEST = "mapMethod"
        const val MAP_ACCOUNT_REQUEST = "mapAccount"
    }
}
