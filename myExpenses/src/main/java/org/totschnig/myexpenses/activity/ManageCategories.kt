/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.ViewModelProvider
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import icepick.State
import org.apache.commons.lang3.ArrayUtils
import org.totschnig.myexpenses.ACTION_MANAGE
import org.totschnig.myexpenses.ACTION_SELECT_FILTER
import org.totschnig.myexpenses.ACTION_SELECT_MAPPING
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.select.SelectMainCategoryDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMainCategoryDialogFragment.CategorySelectedListener
import org.totschnig.myexpenses.fragment.CategoryList
import org.totschnig.myexpenses.model.Category
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.util.ShareUtils
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.ManageCategoriesViewModel

/**
 * SelectCategory activity allows to select categories while editing a transaction
 * and also managing (creating, deleting, importing)
 *
 * @author Michael Totschnig
 */
class ManageCategories : CategoryActivity<CategoryList>(), OnDialogResultListener,
    CategorySelectedListener {
    lateinit var viewModel: ManageCategoriesViewModel

    enum class HelpVariant {
        manage, select_mapping, select_filter
    }

    enum class OperationInProgress {
        Import, Export, Delete
    }

    @State
    @JvmField
    var operationInProgress: OperationInProgress? = null

    private var mCategory: Category? = null
    override fun getAction(): String {
        val intent = intent
        val action = intent.action
        return action ?: ACTION_SELECT_MAPPING
    }

    override fun getContentView(): Int {
        return R.layout.activity_category
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ManageCategoriesViewModel::class.java]
        (applicationContext as MyApplication).appComponent.inject(viewModel)
        val (helpVariant, title) = when (action) {
            Intent.ACTION_MAIN, ACTION_MANAGE ->
                HelpVariant.manage to R.string.pref_manage_categories_title
            ACTION_SELECT_FILTER ->
                HelpVariant.select_filter to R.string.search_category
            ACTION_SELECT_MAPPING ->
                HelpVariant.select_mapping to R.string.select_category
            else -> null to 0
        }
        if (helpVariant != null) setHelpVariant(helpVariant, true)
        if (title != 0) supportActionBar!!.setTitle(title)
        if (action == ACTION_SELECT_MAPPING || action == ACTION_MANAGE) {
            configureFloatingActionButton(R.string.menu_create_main_cat)
        } else {
            findViewById<View>(R.id.CREATE_COMMAND).visibility = View.GONE
        }
        if (savedInstanceState != null && operationInProgress != null) {
            when (operationInProgress) {
                OperationInProgress.Import -> observeImportCatResult()
                OperationInProgress.Export -> observeExportCatResult()
                OperationInProgress.Delete -> observeDeleteResult()
            }
        }
    }

    /*  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final MenuItem item = menu.findItem(R.id.SETUP_CATEGORIES_DEFAULT_COMMAND);
    if (item != null) {
      Utils.menuItemSetEnabledAndVisible(item, getResources().getBoolean(R.bool.has_localized_categories));
    }
    return super.onPrepareOptionsMenu(menu);
  }*/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        val action = action
        if (action != ACTION_SELECT_FILTER) {
            inflater.inflate(R.menu.categories, menu)
        }
        super.onCreateOptionsMenu(menu)
        return true
    }

    private fun isTaskRunning(): Boolean {
        return if (operationInProgress != null) {
            showSnackBar("Previous task still executing, please try again later")
            true
        } else false
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.CREATE_COMMAND -> {
                createCat(null)
                return true
            }
            R.id.DELETE_COMMAND_DO -> {
                if (!isTaskRunning()) {
                    finishActionMode()
                    showSnackBarIndefinite(R.string.progress_dialog_deleting)
                    operationInProgress = OperationInProgress.Delete
                    @Suppress("UNCHECKED_CAST")
                    viewModel.deleteCategories((tag as Array<Long>).toLongArray())
                    observeDeleteResult()

                }
                return true
            }
            R.id.CANCEL_CALLBACK_COMMAND -> {
                finishActionMode()
                return true
            }
            R.id.SETUP_CATEGORIES_DEFAULT_COMMAND -> {
                if (!isTaskRunning()) {
                    showSnackBarIndefinite(R.string.menu_categories_setup_default)
                    viewModel.importCats()
                    observeImportCatResult()
                }
                return true
            }
            R.id.EXPORT_CATEGORIES_COMMAND_ISO88591 -> {
                exportCats("ISO-8859-1")
                return true
            }
            R.id.EXPORT_CATEGORIES_COMMAND_UTF8 -> {
                exportCats("UTF-8")
                return true
            }
            else -> return false
        }
    }

    private fun observeImportCatResult() {
        viewModel.importCatResult?.observe(this) { pair ->
            operationInProgress = null
            showSnackBar(
                if (pair.first == 0 && pair.second == 0) {
                    getString(R.string.import_categories_none)
                } else {
                    buildList {
                        pair.first.takeIf { it != 0 }?.let {
                            add(getString(R.string.import_categories_success, it))
                        }
                        pair.second.takeIf { it != 0 }?.let {
                            add(
                                resources.getQuantityString(
                                    R.plurals.import_categories_icons_updated,
                                    it,
                                    it
                                )
                            )
                        }
                    }.joinToString(separator = " ")
                }
            )
        }
    }

    private fun observeExportCatResult() {
        viewModel.exportCatResult?.observe(this) { result ->
            operationInProgress = null
            result.onSuccess { pair ->
                updateSnackBar(getString(R.string.export_sdcard_success, pair.second))
                if (prefHandler.getBoolean(PrefKey.PERFORM_SHARE, false)) {
                    val shareResult = ShareUtils.share(
                        this, listOf(pair.first),
                        prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim(),
                        "text/qif"
                    )
                    if (!shareResult.isSuccess) {
                        updateSnackBar(shareResult.print(this))
                    }
                }
            }.onFailure {
                updateSnackBar(it.safeMessage)
            }
        }
    }

    private fun observeDeleteResult() {
        viewModel.deleteResult?.observe(this) { result ->
            operationInProgress = null
            result.onSuccess {
                showDismissibleSnackBar(it)
            }.onFailure {
                showDeleteFailureFeedback(it.message)
            }
        }
    }

    private fun exportCats(encoding: String) {
        if (!isTaskRunning()) {
            showDismissibleSnackBar(R.string.menu_categories_export)
            viewModel.exportCats(encoding)
            observeExportCatResult()
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if ((DIALOG_NEW_CATEGORY == dialogTag || DIALOG_EDIT_CATEGORY == dialogTag)
            && which == BUTTON_POSITIVE
        ) {
            var parentId: Long? = null
            if (extras.containsKey(DatabaseConstants.KEY_PARENTID)) {
                parentId = extras.getLong(DatabaseConstants.KEY_PARENTID)
            }
            mCategory = Category(
                extras.getLong(DatabaseConstants.KEY_ROWID),
                extras.getString(DatabaseConstants.KEY_LABEL),
                parentId,
                extras.getInt(DatabaseConstants.KEY_COLOR),
                extras.getString(DatabaseConstants.KEY_ICON)
            )
            startDbWriteTask()
            finishActionMode()
            return true
        }
        return super.onResult(dialogTag, which, extras)
    }

    override fun onCategorySelected(args: Bundle) {
        finishActionMode()
        val target = args.getLong(SelectMainCategoryDialogFragment.KEY_RESULT)
        startTaskExecution(
            TaskExecutionFragment.TASK_MOVE_CATEGORY,
            ArrayUtils.toObject(args.getLongArray(TaskExecutionFragment.KEY_OBJECT_IDS)),
            if (target == 0L) null else target,
            R.string.saving
        )
    }

    override fun onPostExecute(result: Uri?) {
        if (result == null) {
            showSnackBar(
                getString(
                    R.string.already_defined,
                    mCategory?.label ?: ""
                )
            )
        }
        super.onPostExecute(result)
    }

    override fun onPostExecute(taskId: Int, result: Any?) {
        super.onPostExecute(taskId, result)
        if (result !is Result<*>) {
            return
        }
        if (result.isSuccess) {
            when (taskId) {
                TaskExecutionFragment.TASK_MOVE_CATEGORY -> mListFragment.reset()
            }
        }
        val print = result.print0(this)
        print?.let { showSnackBar(it) }
    }

    override fun getObject(): Model {
        return mCategory!!
    }
}