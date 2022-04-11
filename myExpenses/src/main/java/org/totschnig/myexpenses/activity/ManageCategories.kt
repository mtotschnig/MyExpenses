package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.PluralsRes
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SelectColorField
import eltos.simpledialogfragment.form.SelectIconField
import eltos.simpledialogfragment.form.SimpleFormDialog
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.databinding.ActivityComposeFabBinding
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.util.*
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import org.totschnig.myexpenses.viewmodel.CategoryViewModel.DeleteResult.OperationComplete
import org.totschnig.myexpenses.viewmodel.CategoryViewModel.DeleteResult.OperationPending
import org.totschnig.myexpenses.viewmodel.data.Category2

enum class HelpVariant {
    manage, select_mapping, select_filter
}

enum class Action {
    SELECT_MAPPING, SELECT_FILTER, MANAGE
}

open class ManageCategories : ProtectedFragmentActivity(), SimpleDialog.OnDialogResultListener {
    private var actionMode: ActionMode? = null
    val viewModel: CategoryViewModel by viewModels()
    private lateinit var binding: ActivityComposeFabBinding
    private lateinit var sortDelegate: SortDelegate
    private lateinit var choiceMode: ChoiceMode

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (action != Action.SELECT_FILTER) {
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
        sortDelegate.onPrepareOptionsMenu(menu)

        prepareSearch(menu, viewModel.getFilter())
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (sortDelegate.onOptionsItemSelected(item)) {
            invalidateOptionsMenu()
            viewModel.setSortOrder(sortDelegate.currentSortOrder)
            true
        } else super.onOptionsItemSelected(item)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeFabBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(true)
        with((applicationContext as MyApplication).appComponent) {
            inject(viewModel)
        }
        val (helpVariant, title) = when (action) {
            Action.MANAGE ->
                HelpVariant.manage to R.string.pref_manage_categories_title
            Action.SELECT_FILTER ->
                HelpVariant.select_filter to R.string.search_category
            Action.SELECT_MAPPING ->
                HelpVariant.select_mapping to R.string.select_category
        }
        setHelpVariant(helpVariant, true)
        if (title != 0) supportActionBar!!.setTitle(title)
        if (action == Action.SELECT_MAPPING || action == Action.MANAGE) {
            configureFloatingActionButton(R.string.menu_create_main_cat)
        } else {
            findViewById<View>(R.id.CREATE_COMMAND).visibility = View.GONE
        }
        sortDelegate = SortDelegate(
            defaultSortOrder = viewModel.defaultSort,
            prefKey = PrefKey.SORT_ORDER_CATEGORIES,
            options = arrayOf(Sort.LABEL, Sort.USAGES, Sort.LAST_USED),
            prefHandler = prefHandler
        )
        viewModel.setSortOrder(sortDelegate.currentSortOrder)
        observeDeleteResult()
        observeMoveResult()
        observeImportResult()
        observeExportResult()
        binding.composeView.setContent {
            AppTheme(this) {
                choiceMode = when (action) {
                    Action.SELECT_MAPPING -> {
                        val selectionState: MutableState<Category2?> = remember {
                            mutableStateOf(null)
                        }
                        LaunchedEffect(selectionState.value) {
                            selectionState.value?.let {
                                doSingleSelection(it)
                            }
                        }
                        ChoiceMode.SingleChoiceMode(selectionState)
                    }
                    Action.MANAGE, Action.SELECT_FILTER -> {
                        val selectionState = rememberMutableStateListOf<Long>()
                        LaunchedEffect(selectionState.size) {
                            if (selectionState.isNotEmpty()) {
                                startActionMode(selectionState)
                                updateActionModeTitle(selectionState)
                            } else {
                                finishActionMode()
                            }
                        }
                        ChoiceMode.MultiChoiceMode(selectionState, true)
                    }
                }
                viewModel.categoryTree.collectAsState(initial = Category2.EMPTY).value.let { root ->
                    if(root.children.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = CenterHorizontally) {
                                Text(text = stringResource(id = R.string.no_categories))
                                Button(onClick = { importCats() }) {
                                    Column(horizontalAlignment = CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Filled.PlaylistAdd,
                                            contentDescription = null
                                        )
                                        Text(text = stringResource(id = R.string.menu_categories_setup_default))
                                    }
                                }
                            }
                        }
                    } else {
                        Category(
                            category = root,
                            expansionMode = ExpansionMode.DefaultCollapsed(rememberMutableStateListOf()),
                            menuGenerator = { if (action == Action.SELECT_FILTER) null else Menu(
                               listOf(
                                   MenuEntry.edit { editCat(it) },
                                   MenuEntry.delete { viewModel.deleteCategories(listOf(it.id)) },
                                   MenuEntry(
                                       icon = Icons.Filled.Add,
                                       label = stringResource(id = R.string.subcategory)
                                   ) { createCat(it.id) },
                                   MenuEntry(
                                       icon = myiconpack.ArrowsAlt,
                                       label = stringResource(id = R.string.menu_move)
                                   ) { showMoveTargetDialog(it) }
                               )
                            ) },
                            choiceMode = choiceMode
                        )
                    }
                }

            }
        }
    }

    private fun doSingleSelection(category: Category2) {
        val intent = Intent().apply {
            putExtra(KEY_CATID, category.id)
            putExtra(KEY_LABEL, category.path)
            putExtra(DatabaseConstants.KEY_ICON, category.icon)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    fun doMultiSelection() {
        val selected = (choiceMode as ChoiceMode.MultiChoiceMode).selectionState
        val label = viewModel.categoryTree.value.flatten().filter { selected.contains(it.id) }
            .joinToString(separator = ",") { it.path }
        setResult(RESULT_FIRST_USER, Intent().apply {
            putExtra(KEY_CATID, selected.toLongArray())
            putExtra(KEY_LABEL, label)
        })
        finish()
    }

    private fun showMoveTargetDialog(category: Category2) {
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
                    if (action == Action.MANAGE) {
                        menu.add(
                            Menu.NONE,
                            R.id.DELETE_COMMAND,
                            0,
                            R.string.menu_delete
                        ).setIcon(R.drawable.ic_menu_delete)
                    } else if (action == Action.SELECT_FILTER) {
                        menu.add(
                            Menu.NONE,
                            R.id.SELECT_COMMAND,
                            0,
                            R.string.menu_select
                        ).setIcon(R.drawable.ic_menu_done)
                    }
                    return true
                }

                override fun onPrepareActionMode(
                    mode: ActionMode,
                    menu: Menu
                ): Boolean = true

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem
                ): Boolean = when (item.itemId) {
                    R.id.DELETE_COMMAND -> {
                        viewModel.deleteCategories(selectionState)
                        true
                    }
                    R.id.SELECT_COMMAND -> {
                        doMultiSelection()
                        true
                    }
                    else -> false
                }

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
                        showDismissibleSnackBar(
                            if (it) R.string.move_category_success else R.string.move_category_failure,
                            dismissCallback
                        )
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

    private fun observeExportResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportResult.collect { result ->
                    result?.let {
                        result.onSuccess { pair ->
                            updateSnackBar(getString(R.string.export_sdcard_success, pair.second))
                            if (prefHandler.getBoolean(PrefKey.PERFORM_SHARE, false)) {
                                val shareResult = ShareUtils.share(
                                    this@ManageCategories, listOf(pair.first),
                                    prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim(),
                                    "text/qif"
                                )
                                if (!shareResult.isSuccess) {
                                    updateSnackBar(shareResult.print(this@ManageCategories))
                                }
                            }
                        }.onFailure {
                            updateSnackBar(it.safeMessage)
                        }
                    }
                }
            }
        }
    }

    private fun observeImportResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importResult.collect { pair ->
                    pair?.let {
                        showDismissibleSnackBar(
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
                            },
                            dismissCallback
                        )
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
            R.id.SETUP_CATEGORIES_DEFAULT_COMMAND -> {
                importCats()
                true
            }

            R.id.EXPORT_CATEGORIES_COMMAND_ISO88591 -> {
                exportCats("ISO-8859-1")
                true
            }
            R.id.EXPORT_CATEGORIES_COMMAND_UTF8 -> {
                exportCats("UTF-8")
                true
            }
            else -> false
        }

    private fun importCats() {
        showSnackBarIndefinite(R.string.menu_categories_setup_default)
        viewModel.importCats()
    }

    private fun exportCats(encoding: String) {
        showDismissibleSnackBar(R.string.menu_categories_export)
        viewModel.exportCats(encoding)
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
            .show(this, DIALOG_NEW_CATEGORY)
    }

    /**
     * presents AlertDialog for editing an existing category
     */
    open fun editCat(category: Category2) {
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
            .show(this, DIALOG_EDIT_CATEGORY)
    }

    private fun buildLabelField(text: String?) =
        Input.plain(KEY_LABEL).required().hint(R.string.label).text(text)
            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

    private fun buildIconField(preset: String?) =
        SelectIconField.picker(DatabaseConstants.KEY_ICON).icons(BuildConfig.CATEGORY_ICONS)
            .preset(preset).label(R.string.icon)

    override fun onResult(dialogTag: String, which: Int, extras: Bundle) =
        if ((DIALOG_NEW_CATEGORY == dialogTag || DIALOG_EDIT_CATEGORY == dialogTag)
            && which == BUTTON_POSITIVE
        ) {
            val label = extras.getString(KEY_LABEL)!!
            viewModel.saveCategory(
                Category2(
                    id = extras.getLong(DatabaseConstants.KEY_ROWID),
                    label = label,
                    parentId = extras.getLong(DatabaseConstants.KEY_PARENTID).takeIf { it != 0L },
                    color = extras.getInt(DatabaseConstants.KEY_COLOR),
                    icon = extras.getString(DatabaseConstants.KEY_ICON)
                )
            ).observe(this) { result ->
                if (result == null) {
                    showSnackBar(getString(R.string.already_defined, label))
                }
            }
            true
        } else false

    val action get() = enumValueOrDefault(intent.action, Action.SELECT_MAPPING)

    companion object {
        const val DIALOG_NEW_CATEGORY = "dialogNewCat"
        const val DIALOG_EDIT_CATEGORY = "dialogEditCat"
    }
}