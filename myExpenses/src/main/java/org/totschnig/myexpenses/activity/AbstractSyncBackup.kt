package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.input.SimpleInputDialog
import eltos.simpledialogfragment.list.CustomListDialog.SINGLE_CHOICE
import eltos.simpledialogfragment.list.SimpleListDialog
import icepick.Icepick
import org.totschnig.myexpenses.R

const val DIALOG_TAG_FOLDER_SELECT = "FOLDER_SELECT"
const val DIALOG_TAG_FOLDER_CREATE = "FOLDER_CREATE"

abstract class AbstractSyncBackup: ProtectedFragmentActivity(), SimpleDialog.OnDialogResultListener {

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
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_FOLDER_SELECT) == null) {
            SimpleListDialog.build().choiceMode(SINGLE_CHOICE)
                    .title(R.string.synchronization_select_folder_dialog_title)
                    .items(names.toTypedArray(), LongArray(names.size) { it.toLong() })
                    .neg()
                    .pos(R.string.select)
                    .neut(R.string.menu_create_folder)
                    .show(this, DIALOG_TAG_FOLDER_SELECT)
        }
    }

    @SuppressLint("BuildNotImplemented")
    fun showCreateFolderDialog() {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_FOLDER_CREATE) == null) {
            SimpleInputDialog.build()
                    .title(R.string.menu_create_folder)
                    .pos(android.R.string.ok)
                    .neut()
                    .show(this, DIALOG_TAG_FOLDER_CREATE)
        }
    }

    abstract fun onFolderSelect(extras: Bundle)
    abstract fun onFolderCreate(label: String)

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when {
            dialogTag.equals(DIALOG_TAG_FOLDER_SELECT) -> {
                when (which) {
                    SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                        onFolderSelect(extras)
                    }
                    SimpleDialog.OnDialogResultListener.BUTTON_NEUTRAL -> showCreateFolderDialog()
                    SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> abort()
                }
                return true
            }
            dialogTag.equals(DIALOG_TAG_FOLDER_CREATE) -> {
                when (which) {
                    SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                        onFolderCreate(extras.getString(SimpleInputDialog.TEXT, "MyExpenses"))
                    }
                    SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> abort()
                }
                return true
            }
        }
        return false
    }

    private fun abort() {
        setResult(Activity.RESULT_CANCELED)
        finish();
    }
}
