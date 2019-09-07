package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.os.Bundle
import eltos.simpledialogfragment.input.SimpleInputDialog
import eltos.simpledialogfragment.list.CustomListDialog.SINGLE_CHOICE
import eltos.simpledialogfragment.list.SimpleListDialog
import icepick.Icepick
import org.totschnig.myexpenses.R

abstract class AbstractSyncBackup: ProtectedFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    @SuppressLint("BuildNotImplemented")
    protected fun showSelectFolderDialog(names: List<String>) {
        SimpleListDialog.build().choiceMode(SINGLE_CHOICE)
                .title(R.string.synchronization_select_folder_dialog_title)
                .items(names.toTypedArray(), LongArray(names.size) { it.toLong() })
                .neg()
                .pos(R.string.select)
                .neut(R.string.menu_create_folder)
                .show(this, DIALOG_TAG_FOLDER_SELECT)
    }

    @SuppressLint("BuildNotImplemented")
    fun showCreateFolderDialog() {
        SimpleInputDialog.build()
                .title(R.string.menu_create_folder)
                .pos(android.R.string.ok)
                .neut()
                .show(this, DIALOG_TAG_FOLDER_CREATE)
    }
}
