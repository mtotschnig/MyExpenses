package org.totschnig.myexpenses.activity

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.input.SimpleInputDialog
import eltos.simpledialogfragment.list.CustomListDialog
import eltos.simpledialogfragment.list.CustomListDialog.SINGLE_CHOICE
import eltos.simpledialogfragment.list.SimpleListDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AbstractSetupViewModel

const val DIALOG_TAG_FOLDER_SELECT = "FOLDER_SELECT"
const val DIALOG_TAG_FOLDER_CREATE = "FOLDER_CREATE"

abstract class AbstractSyncSetup<T : AbstractSetupViewModel> : ProtectedFragmentActivity(),
    SimpleDialog.OnDialogResultListener {
    lateinit var viewModel: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = instantiateViewModel()
        if (savedInstanceState == null || !viewModel.folderList.isInitialized) {
            viewModel.folderList.observe(this) {
                if (it.isNotEmpty()) {
                    showSelectFolderDialog(it)
                } else {
                    showCreateFolderDialog()
                }
            }
        }
        viewModel.folderCreateResult.observe(this) {
            success(it)
        }
        viewModel.error.observe(this) { exception ->
            if (!handleException(exception)) {
                CrashHandler.report(exception)
                showMessage(exception.safeMessage)
            }
        }
    }

    override fun onMessageDialogDismissOrCancel() {
        abort()
    }

    abstract fun handleException(exception: Exception): Boolean

    abstract fun instantiateViewModel(): T

    fun success(folder: Pair<String, String>) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(AccountManager.KEY_ACCOUNT_NAME, viewModel.backendService.buildAccountName(folder.second))
            buildSuccessIntent(folder) }
        )
        finish()
    }

    abstract fun Intent.buildSuccessIntent(folder: Pair<String, String>)

    private fun showSelectFolderDialog(pairs: List<Pair<String, String>>) {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_FOLDER_SELECT) == null) {
            SimpleListDialog.build().choiceMode(SINGLE_CHOICE)
                .title(R.string.synchronization_select_folder_dialog_title)
                .choiceMin(1)
                .items(
                    pairs.map { pair -> pair.second }.toTypedArray(),
                    LongArray(pairs.size) { it.toLong() })
                .neg()
                .pos(R.string.select)
                .neut(R.string.menu_create_folder)
                .show(this, DIALOG_TAG_FOLDER_SELECT)
        }
    }

    private fun showCreateFolderDialog() {
        if (supportFragmentManager.findFragmentByTag(DIALOG_TAG_FOLDER_CREATE) == null) {
            SimpleInputDialog.build()
                .title(R.string.menu_create_folder)
                .pos(android.R.string.ok)
                .neg()
                .show(this, DIALOG_TAG_FOLDER_CREATE)
        }
    }

    private fun onFolderCreate(label: String) {
        viewModel.createFolder(label)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (dialogTag) {
            DIALOG_TAG_FOLDER_SELECT -> {
                when (which) {
                    BUTTON_POSITIVE -> {
                        extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL)?.let {
                            success(
                                viewModel.folderList.value!![extras.getInt(CustomListDialog.SELECTED_SINGLE_POSITION)]
                            )
                        } ?: run {
                            Toast.makeText(
                                this,
                                "Could not find folder label in result",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    BUTTON_NEUTRAL -> showCreateFolderDialog()
                    BUTTON_NEGATIVE -> abort()
                }
                return true
            }
            DIALOG_TAG_FOLDER_CREATE -> {
                when (which) {
                    BUTTON_POSITIVE -> {
                        onFolderCreate(extras.getString(SimpleInputDialog.TEXT, "MyExpenses"))
                    }
                    BUTTON_NEGATIVE -> abort()
                }
                return true
            }
        }
        return false
    }

    protected fun abort() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
