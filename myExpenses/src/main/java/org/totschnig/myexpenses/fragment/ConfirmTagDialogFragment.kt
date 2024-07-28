package org.totschnig.myexpenses.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat.getParcelableArrayList
import androidx.fragment.app.setFragmentResult
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TagHandler
import org.totschnig.myexpenses.activity.TagHandler.Companion.CONFIRM_MAP_TAG_REQUEST
import org.totschnig.myexpenses.dialog.BaseDialogFragment
import org.totschnig.myexpenses.fragment.TagList.Companion.KEY_TAG_LIST
import org.totschnig.myexpenses.viewmodel.data.Tag


class ConfirmTagDialogFragment : BaseDialogFragment() {
    val tagList
        get() = getParcelableArrayList(requireArguments(), KEY_TAG_LIST, Tag::class.java)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isEmpty = tagList.size == 0
        return initBuilder().also {
            it.setTitle(
                    if (isEmpty) getString(R.string.dialog_multi_tag_clear) else getString(
                        R.string.dialog_multi_tag,
                        tagList.joinToString(", ") { tag -> tag.label })
                )
                .setNegativeButton(android.R.string.cancel, null)
            if (isEmpty) {
                it.setPositiveButton(R.string.remove) { _: DialogInterface, _: Int ->
                    confirm(
                        true
                    )
                }
            } else {
                it.setSingleChoiceItems(R.array.multi_tag_options, 0, null)
                    .setPositiveButton(R.string.menu_tag) { _: DialogInterface, _: Int ->
                    confirm((dialog as AlertDialog).listView.checkedItemPosition == 1)
                }
            }
        }.create()
    }

    private fun confirm(replace: Boolean) {
        setFragmentResult(CONFIRM_MAP_TAG_REQUEST, Bundle(2).apply {
            putBoolean(TagHandler.KEY_REPLACE, replace)
            putParcelableArrayList(KEY_TAG_LIST, tagList)
        })
    }
}