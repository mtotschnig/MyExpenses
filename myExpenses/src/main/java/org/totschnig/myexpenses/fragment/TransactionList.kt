package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.CONFIRM_MAP_TAG_REQUEST
import org.totschnig.myexpenses.activity.MAP_TAG_REQUEST
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.dialog.TransactionDetailFragment
import org.totschnig.myexpenses.viewmodel.data.Tag

const val KEY_REPLACE = "replace"

class TransactionList : BaseTransactionList() {
    private fun handleTagResult(intent: Intent) {
        ConfirmTagDialogFragment().also {
            it.arguments = Bundle().apply {
                putParcelableArrayList(KEY_TAG_LIST, intent.getParcelableArrayListExtra(KEY_TAG_LIST))
            }
            it.setTargetFragment(this, CONFIRM_MAP_TAG_REQUEST)
        }.show(parentFragmentManager, "CONFIRM")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == CONFIRM_MAP_TAG_REQUEST) {
                intent?.let {
                    viewModel.tag(mListView.checkedItemIds, it.getParcelableArrayListExtra(KEY_TAG_LIST)!!, it.getBooleanExtra(KEY_REPLACE, false))
                }
                finishActionMode()
            } else if (requestCode == MAP_TAG_REQUEST) {
                handleTagResult(intent!!)
            } else {
                super.onActivityResult(requestCode, resultCode, intent)
            }
        }
    }

    override fun configureMenuInternal(menu: Menu, hasSplit: Boolean, hasVoid: Boolean, hasNotSplit: Boolean, hasTransfer: Boolean, count: Int) {
        with(menu) {
            findItem(R.id.CREATE_TEMPLATE_COMMAND).isVisible = count == 1
            findItem(R.id.SPLIT_TRANSACTION_COMMAND).isVisible = !hasSplit && !hasVoid
            findItem(R.id.UNGROUP_SPLIT_COMMAND).isVisible = !hasNotSplit && !hasVoid
            findItem(R.id.UNDELETE_COMMAND).isVisible = hasVoid
            findItem(R.id.EDIT_COMMAND).isVisible = count == 1 && !hasVoid
            findItem(R.id.REMAP_ACCOUNT_COMMAND).isVisible = (activity as? MyExpenses)?.accountCount ?: 0 > 1
            findItem(R.id.REMAP_PAYEE_COMMAND).isVisible = !hasTransfer
            findItem(R.id.REMAP_CATEGORY_COMMAND).isVisible = !hasTransfer && !hasSplit
            findItem(R.id.REMAP_METHOD_COMMAND).isVisible = !hasTransfer
        }
    }

    override fun showDetails(transactionId: Long) {
        lifecycleScope.launchWhenResumed {
            with(parentFragmentManager)  {
                if (findFragmentByTag(TransactionDetailFragment::class.java.name) == null) {
                    TransactionDetailFragment.newInstance(transactionId).show(this, TransactionDetailFragment::class.java.name)
                }
            }
        }
    }
}

class ConfirmTagDialogFragment : DialogFragment() {
    val tagList
        get() = requireArguments().getParcelableArrayList<Tag>(KEY_TAG_LIST)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isEmpty = tagList.size == 0
        val dialog = MaterialDialog(requireContext())
                .title(R.string.menu_tag)
                .message(text = if (isEmpty) getString(R.string.dialog_multi_tag_clear) else getString(R.string.dialog_multi_tag, tagList.map { tag -> tag.label }.joinToString(", ")))
                .negativeButton(android.R.string.cancel)
        return if (isEmpty) {
            dialog.positiveButton(R.string.menu_remove) { confirm(true) }
        } else {
            dialog.listItemsSingleChoice(R.array.multi_tag_options) { _, index, _ -> confirm(index == 1) }
                    .positiveButton(R.string.menu_tag)
        }
    }

    private fun confirm(replace: Boolean) {
        targetFragment?.onActivityResult(CONFIRM_MAP_TAG_REQUEST, Activity.RESULT_OK, Intent().apply {
            putExtra(KEY_REPLACE, replace)
            putParcelableArrayListExtra(KEY_TAG_LIST, tagList)
        })
    }
}