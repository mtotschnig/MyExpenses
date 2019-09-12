package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.input.SimpleInputDialog
import eltos.simpledialogfragment.list.CustomListDialog
import eltos.simpledialogfragment.list.CustomListDialog.SINGLE_CHOICE
import eltos.simpledialogfragment.list.SimpleListDialog
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel

const val DIALOG_TAG_FOLDER_SELECT = "FOLDER_SELECT"
const val DIALOG_TAG_FOLDER_CREATE = "FOLDER_CREATE"

abstract class AbstractSyncBackup<T : AbstractSetupViewModel> : ProtectedFragmentActivity(), SimpleDialog.OnDialogResultListener {
    lateinit var viewModel: T
    @JvmField
    @State
    var idList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)
        viewModel = instantiateViewModel()
        viewModel.folderList.observe(this, Observer {
            if (it.size > 0) {
                showSelectFolderDialog(it)
            } else {
                showCreateFolderDialog()
            }
        })
        viewModel.folderCreateResult.observe(this, Observer {
            success(it)
        })
        viewModel.error.observe(this, Observer {
            if (!handleException(it)) {
                CrashHandler.report(it)
                it.message?.let {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
                finish()
            }
        })
    }

    abstract fun handleException(exception: Exception): Boolean

    abstract fun instantiateViewModel(): T

    fun success(folder: Pair<String, String>) {
        setResult(RESULT_OK, buildSuccessIntent(folder))
        finish()
    }

    abstract fun buildSuccessIntent(folder: Pair<String, String>): Intent

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    @SuppressLint("BuildNotImplemented")
    protected fun showSelectFolderDialog(pairs: List<Pair<String, String>>) {
        idList.clear()
        idList.addAll(pairs.map { pair -> pair.first })
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_FOLDER_SELECT) == null) {
            SimpleListDialog.build().choiceMode(SINGLE_CHOICE)
                    .title(R.string.synchronization_select_folder_dialog_title)
                    .items(pairs.map { pair -> pair.second }.toTypedArray(), LongArray(pairs.size) { it.toLong() })
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
                    .neg()
                    .show(this, DIALOG_TAG_FOLDER_CREATE)
        }
    }

    fun onFolderCreate(label: String) {
        viewModel.createFolder(label)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when {
            dialogTag.equals(DIALOG_TAG_FOLDER_SELECT) -> {
                when (which) {
                    SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                        success(Pair(idList.get(extras.getLong(CustomListDialog.SELECTED_SINGLE_ID).toInt()),
                                extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL)))
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
