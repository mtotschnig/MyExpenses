package org.totschnig.myexpenses.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TagHandler
import org.totschnig.myexpenses.activity.TagHandler.Companion.CONFIRM_MAP_TAG_REQUEST
import org.totschnig.myexpenses.fragment.TagList.Companion.KEY_TAG_LIST
import org.totschnig.myexpenses.viewmodel.data.Tag

class ConfirmTagDialogFragment : DialogFragment() {
    val tagList
        get() = requireArguments().getParcelableArrayList<Tag>(KEY_TAG_LIST)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isEmpty = tagList.size == 0
        val dialog = MaterialDialog(requireContext())
            .title(R.string.menu_tag)
            .message(
                text = if (isEmpty) getString(R.string.dialog_multi_tag_clear) else getString(
                    R.string.dialog_multi_tag,
                    tagList.joinToString(", ") { tag -> tag.label })
            )
            .negativeButton(android.R.string.cancel)
        return if (isEmpty) {
            dialog.positiveButton(R.string.menu_remove) { confirm(true) }
        } else {
            dialog.listItemsSingleChoice(R.array.multi_tag_options) { _, index, _ -> confirm(index == 1) }
                .positiveButton(R.string.menu_tag)
        }
    }

    private fun confirm(replace: Boolean) {
        setFragmentResult(CONFIRM_MAP_TAG_REQUEST, Bundle(2).apply {
            putBoolean(TagHandler.KEY_REPLACE, replace)
            putParcelableArrayList(KEY_TAG_LIST, tagList)
        })
    }
}