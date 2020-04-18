package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity.CONFIRM_MAP_TAG_RQEUST
import org.totschnig.myexpenses.viewmodel.data.Tag

const val KEY_REPLACE = "replace"

class TransactionList : BaseTransactionList() {
    override fun handleTagResult(intent: Intent) {
        ConfirmTagDialogFragment().also {
            it.arguments = Bundle().apply {
                putParcelableArrayList(KEY_TAGLIST, intent.getParcelableArrayListExtra(KEY_TAGLIST))
            }
            it.setTargetFragment(this, CONFIRM_MAP_TAG_RQEUST)
        }.show(parentFragmentManager, "CONFIRM")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == CONFIRM_MAP_TAG_RQEUST) {
            if (resultCode == Activity.RESULT_OK) {
                intent?.let {
                    viewModel.tag(mListView.checkedItemIds, it.getParcelableArrayListExtra(KEY_TAGLIST)!!, it.getBooleanExtra(KEY_REPLACE, false))
                }
            }
            finishActionMode()
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }
}

class ConfirmTagDialogFragment : DialogFragment() {
    val tagList
        get() = arguments!!.getParcelableArrayList<Tag>(KEY_TAGLIST)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isEmpty = tagList.size == 0
        val dialog = MaterialDialog(context!!)
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
        targetFragment?.onActivityResult(CONFIRM_MAP_TAG_RQEUST, Activity.RESULT_OK, Intent().apply {
            putExtra(KEY_REPLACE, replace)
            putParcelableArrayListExtra(KEY_TAGLIST, tagList)
        })
    }
}