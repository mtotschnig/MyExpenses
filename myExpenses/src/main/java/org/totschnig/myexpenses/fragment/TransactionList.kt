package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
                    finishActionMode()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }
}

class ConfirmTagDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val tagList = arguments!!.getParcelableArrayList<Tag>(KEY_TAGLIST)!!
        return MaterialDialog(context!!)
                .title(R.string.menu_tag)
                .message(text = getString(R.string.dialog_multi_tag, tagList.map { tag -> tag.label }.joinToString(", ")))
                .listItemsSingleChoice(R.array.multi_tag_options) { _, index, _ ->
                    targetFragment?.onActivityResult(CONFIRM_MAP_TAG_RQEUST, Activity.RESULT_OK, Intent().apply {
                        putExtra(KEY_REPLACE, index == 1)
                        putParcelableArrayListExtra(KEY_TAGLIST, tagList)
                    })
                }
                .positiveButton(R.string.menu_tag)
    }
}