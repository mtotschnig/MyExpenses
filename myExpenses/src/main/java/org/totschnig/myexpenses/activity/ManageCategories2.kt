package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.PluralsRes
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SelectColorField
import eltos.simpledialogfragment.form.SelectIconField
import eltos.simpledialogfragment.form.SimpleFormDialog
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.*
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.databinding.ActivityCategoryComposeBinding
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForCategories
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import org.totschnig.myexpenses.viewmodel.CategoryViewModel.DeleteResult.OperationComplete
import org.totschnig.myexpenses.viewmodel.CategoryViewModel.DeleteResult.OperationPending

open class ManageCategories2 : ProtectedFragmentActivity(), SimpleDialog.OnDialogResultListener {
    private var actionMode: ActionMode? = null
    val viewModel: CategoryViewModel by viewModels()
    private lateinit var binding: ActivityCategoryComposeBinding
    private val sortOrder: String
        get() = preferredOrderByForCategories(
            getSortOrderPrefKey(),
            prefHandler,
            defaultSortOrder
        )!!

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (action != ACTION_SELECT_FILTER) {
            menuInflater.inflate(R.menu.categories, menu)
        }
        menuInflater.inflate(R.menu.search, menu)
        configureSearch(this, menu, this::onQueryTextChange)
        super.onCreateOptionsMenu(menu)
        return true
    }

    private fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setFilter(
            if (newText.isNullOrEmpty()) {
                ""
                //configureImportButton(true)
            } else {
                newText
                // if a filter results in an empty list,
                // we do not want to show the setup default categories button
                //configureImportButton(false)
            }
        )
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.findItem(R.id.SORT_COMMAND)
        if (menuItem != null) {
            val currentItem = menuItem.subMenu.findItem(getCurrentSortOrder().commandId)
            if (currentItem != null) {
                currentItem.isChecked = true
            }
        }
        prepareSearch(menu, viewModel.getFilter())
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = Sort.fromCommandId(item.itemId)?.let {
        if (!item.isChecked) {
            prefHandler.putString(getSortOrderPrefKey(), it.name)
            invalidateOptionsMenu()
            viewModel.setSortOrder(sortOrder)
        }
        true
    } ?: super.onOptionsItemSelected(item)

    protected val defaultSortOrder get() = Sort.USAGES

    protected fun getCurrentSortOrder() =
        enumValueOrDefault(
            prefHandler.getString(getSortOrderPrefKey(), null),
            defaultSortOrder
        )

    protected fun getSortOrderPrefKey() = PrefKey.SORT_ORDER_CATEGORIES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        viewModel.setSortOrder(sortOrder)
        observeDeleteResult()
        observeMoveResult()
        binding.composeView.setContent {
            AppTheme(this) {
                val selectionState = rememberMutableStateListOf<Long>()
                LaunchedEffect(selectionState.size) {
                    if (selectionState.isNotEmpty()) {
                        startActionMode(selectionState)
                        updateActionModeTitle(selectionState)
                    } else {
                        finishActionMode()
                    }
                }
                Category(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.general_padding)),
                    category = viewModel.categoryTree.collectAsState(initial = Category.EMPTY).value,
                    expansionMode = ExpansionMode.DefaultCollapsed(rememberMutableStateListOf()),
                    onEdit = { editCat(it) },
                    onDelete = { viewModel.deleteCategories(listOf(it)) },
                    onAdd = { createCat(it) },
                    onMove = { showMoveTargetDialog(it) },
                    choiceMode = ChoiceMode.MultiChoiceMode(selectionState, true)
                )
            }
        }
    }

    private fun showMoveTargetDialog(category: Category) {
        SelectCategoryMoveTargetDialogFragment.newInstance(category)
            .show(supportFragmentManager, "SELECT_TARGET")
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun updateActionModeTitle(selectionState: SnapshotStateList<Long>) {
        actionMode?.title = "${selectionState.size}"
    }

    private fun startActionMode(selectionState: SnapshotStateList<Long>) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu
                ): Boolean {
                    menu.add(
                        Menu.NONE,
                        R.id.DELETE_COMMAND,
                        0,
                        R.string.menu_delete
                    )
                    return true
                }

                override fun onPrepareActionMode(
                    mode: ActionMode,
                    menu: Menu
                ): Boolean = true

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem
                ): Boolean = if (item.itemId == R.id.DELETE_COMMAND) {
                    viewModel.deleteCategories(selectionState)
                    true
                } else false

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                    selectionState.clear()
                }

            })
        }
    }

    private fun MutableList<String>.mapToMessage(quantity: Int, @PluralsRes resId: Int) {
        if (quantity > 0) add(
            resources.getQuantityString(
                resId,
                quantity,
                quantity
            )
        )
    }

    private val dismissCallback = object : Snackbar.Callback() {
        override fun onDismissed(
            transientBottomBar: Snackbar,
            event: Int
        ) {
            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                viewModel.messageShown()
        }
    }

    private fun observeMoveResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.moveResult.collect { result ->
                    result?.let {
                        showDismissibleSnackBar(if (it) R.string.move_category_success else R.string.move_category_success, dismissCallback)
                    }
                }
            }
        }
    }

    private fun observeDeleteResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteResult.collect { result ->
                    result?.onSuccess {
                        when (it) {
                            is OperationComplete -> {
                                finishActionMode()
                                val messages = buildList {
                                    mapToMessage(it.deleted, R.plurals.delete_success)
                                    mapToMessage(
                                        it.mappedToTransactions,
                                        R.plurals.not_deletable_mapped_transactions
                                    )
                                    mapToMessage(
                                        it.mappedToTemplates,
                                        R.plurals.not_deletable_mapped_templates
                                    )
                                }
                                showDismissibleSnackBar(
                                    messages.joinToString(" "),
                                    dismissCallback
                                )
                            }
                            is OperationPending -> {
                                val messages = buildList {
                                    mapToMessage(
                                        it.hasDescendants,
                                        R.plurals.warning_delete_main_category
                                    )
                                    if (it.mappedToBudgets > 0) {
                                        add(getString(R.string.warning_delete_category_with_budget))
                                    }
                                    add(getString(R.string.continue_confirmation))
                                }
                                MessageDialogFragment.newInstance(
                                    getString(R.string.dialog_title_warning_delete_category),
                                    messages.joinToString(" "),
                                    MessageDialogFragment.Button(
                                        R.string.response_yes,
                                        R.id.DELETE_COMMAND_DO,
                                        it.ids.toTypedArray()
                                    ),
                                    null,
                                    MessageDialogFragment.Button(
                                        R.string.response_no,
                                        R.id.CANCEL_CALLBACK_COMMAND,
                                        null
                                    )
                                )
                                    .show(supportFragmentManager, "DELETE_CATEGORY")
                                viewModel.messageShown()
                            }
                        }
                    }?.onFailure {
                        showDeleteFailureFeedback(it.message, dismissCallback)
                    }
                }
            }
        }
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
            R.id.CREATE_COMMAND -> {
                createCat(null)
                true
            }
            R.id.CANCEL_CALLBACK_COMMAND -> {
                finishActionMode()
                true
            }
            R.id.DELETE_COMMAND_DO -> {
                showSnackBarIndefinite(R.string.progress_dialog_deleting)
                @Suppress("UNCHECKED_CAST")
                viewModel.deleteCategoriesDo((tag as Array<Long>).toList())
                true
            }
            else -> false
        }

    /**
     * presents AlertDialog for adding a new category
     * if label is already used, shows an error
     */
    open fun createCat(parentId: Long?) {
        val args = Bundle()
        if (parentId != null) {
            args.putLong(DatabaseConstants.KEY_PARENTID, parentId)
        }
        SimpleFormDialog.build()
            .title(if (parentId == null) R.string.menu_create_main_cat else R.string.menu_create_sub_cat)
            .cancelable(false)
            .fields(buildLabelField(null), buildIconField(null))
            .pos(R.string.dialog_button_add)
            .neut()
            .extra(args)
            .show(this, CategoryActivity.DIALOG_NEW_CATEGORY)
    }

    /**
     * presents AlertDialog for editing an existing category
     */
    open fun editCat(category: Category) {
        val args = Bundle().apply {
            putLong(DatabaseConstants.KEY_ROWID, category.id)
        }
        val formElements = buildList {
            add(buildLabelField(category.label))
            if (category.level == 1 && category.color != null) {
                add(
                    SelectColorField.picker(DatabaseConstants.KEY_COLOR).label(R.string.color)
                        .color(category.color)
                )
            }
            add(buildIconField(category.icon))
        }.toTypedArray()

        SimpleFormDialog.build()
            .title(R.string.menu_edit_cat)
            .cancelable(false)
            .fields(*formElements)
            .pos(R.string.menu_save)
            .neut()
            .extra(args)
            .show(this, CategoryActivity.DIALOG_EDIT_CATEGORY)
    }

    private fun buildLabelField(text: String?) =
        Input.plain(DatabaseConstants.KEY_LABEL).required().hint(R.string.label).text(text)
            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

    private fun buildIconField(preset: String?) =
        SelectIconField.picker(DatabaseConstants.KEY_ICON).icons(BuildConfig.CATEGORY_ICONS)
            .preset(preset).label(R.string.icon)

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
        if ((CategoryActivity.DIALOG_NEW_CATEGORY == dialogTag || CategoryActivity.DIALOG_EDIT_CATEGORY == dialogTag)
            && which == CategoryActivity.BUTTON_POSITIVE
        ) {
            val parentId = if (extras.containsKey(DatabaseConstants.KEY_PARENTID)) {
                extras.getLong(DatabaseConstants.KEY_PARENTID)
            } else null
            val label = extras.getString(DatabaseConstants.KEY_LABEL)
            viewModel.saveCategory(
                org.totschnig.myexpenses.model.Category(
                    extras.getLong(DatabaseConstants.KEY_ROWID),
                    label,
                    parentId,
                    extras.getInt(DatabaseConstants.KEY_COLOR),
                    extras.getString(DatabaseConstants.KEY_ICON)
                )
            ).observe(this) { result ->
                if (result == null) {
                    showSnackBar(getString(R.string.already_defined, label))
                }
            }
            true
        } else false

    val action get() = intent.action ?: ACTION_SELECT_MAPPING
}