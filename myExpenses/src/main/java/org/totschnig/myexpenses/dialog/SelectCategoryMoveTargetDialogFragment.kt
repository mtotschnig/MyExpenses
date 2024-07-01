package org.totschnig.myexpenses.dialog

import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.data.Category

class SelectCategoryMoveTargetDialogFragment: SelectCategoryBaseDialogFragment() {

    val source: Category
        get() = requireArguments().getParcelable(KEY_SOURCE)!!

    override val withRoot: Int?
        get() = if (source.parentId != null) R.string.transform_subcategory_to_main else null

    override val titleResId = R.string.dialog_title_select_target

    override val excludedSubTree: Long
        get() = source.id

    override fun isSelectable(id: Long) = id != source.parentId
    override fun actionButtonLabel(selection: Category?) = if (selection?.id == 0L)
         selection.label
     else
         "${getString(R.string.menu_move)} (${source.label} -> ${selection?.label ?: "?"})"

    override fun onActionButtonClick(value: Category) {
        viewModel.moveCategory(
            source.id,
            value.id.takeIf { it != 0L }
        )
    }

    companion object {
        const val KEY_SOURCE = "source"
        fun newInstance(category: Category) = SelectCategoryMoveTargetDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_SOURCE, category)
            }
        }
    }

}