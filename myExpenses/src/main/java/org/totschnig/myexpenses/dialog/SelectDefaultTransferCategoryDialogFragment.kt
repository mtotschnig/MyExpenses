package org.totschnig.myexpenses.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.viewmodel.data.Category

class SelectDefaultTransferCategoryDialogFragment: SelectCategoryBaseDialogFragment() {
    override val titleResId: Int
        get() = R.string.default_transfer_category

    override val withRoot: Int
        get() = R.string.unmapped

    override fun actionButtonLabel(selection: Category?) = getString(R.string.select)

    override fun onActionButtonClick(value: Category) {
        if (value.id > 0) {
            prefHandler.putLong(PrefKey.DEFAULT_TRANSFER_CATEGORY, value.id)
        } else {
            prefHandler.remove(PrefKey.DEFAULT_TRANSFER_CATEGORY)
        }
        setFragmentResult(SELECT_CATEGORY_REQUEST, bundleOf(
            KEY_PATH to if (value.id > 0) value.path else getString(withRoot)
        ))
    }

    companion object {
        const val SELECT_CATEGORY_REQUEST = "selectCategory"
    }
}