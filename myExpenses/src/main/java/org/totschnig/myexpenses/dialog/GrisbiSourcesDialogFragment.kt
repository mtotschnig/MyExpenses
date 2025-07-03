package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.GrisbiImport

class GrisbiSourcesDialogFragment : TextSourceDialogFragment(), DialogInterface.OnClickListener {
    override val layoutId = R.layout.grisbi_import_dialog
    override val layoutTitle: String
        get() = getString(R.string.pref_import_from_grisbi_title)

    override val typeName = "Grisbi XML"

    override val prefKey = "import_grisbi_file_uri"

    override fun onClick(dialog: DialogInterface, id: Int) {
        if (activity == null) {
            return
        }
        if (id == AlertDialog.BUTTON_POSITIVE) {
            maybePersistUri()
            (activity as GrisbiImport?)!!.onSourceSelected(
                uri,
                mImportCategories.isChecked,
                mImportParties.isChecked
            )
        } else {
            super.onClick(dialog, id)
        }
    }

    override fun setupDialogView(view: View) {
        super.setupDialogView(view)
        mImportTransactions.visibility = View.GONE
    }

    companion object {
        @JvmStatic
        fun newInstance(): GrisbiSourcesDialogFragment {
            return GrisbiSourcesDialogFragment()
        }
    }
}