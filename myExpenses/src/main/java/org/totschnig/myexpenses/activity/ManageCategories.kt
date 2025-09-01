package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.PluralsRes
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.select
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.dialog.SelectCategoryMoveTargetDialogFragment
import org.totschnig.myexpenses.dialog.SetupCategoriesConfirmDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.provider.filter.preSelected
import org.totschnig.myexpenses.util.checkMenuIcon
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.util.prepareSync
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.viewmodel.CategoryViewModel
import org.totschnig.myexpenses.viewmodel.CategoryViewModel.DeleteResult.OperationComplete
import org.totschnig.myexpenses.viewmodel.CategoryViewModel.DeleteResult.OperationPending
import org.totschnig.myexpenses.viewmodel.LoadingState
import org.totschnig.myexpenses.viewmodel.data.Category
import java.io.Serializable

class ManageCategories : ProtectedFragmentActivity(),
    ContribIFace {

    private var actionMode: ActionMode? = null
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var binding: ActivityComposeBinding
    private lateinit var sortDelegate: SortDelegate
    private lateinit var choiceMode: ChoiceMode
    private val parentSelectionOnTap: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val action = intent.asAction
        if (action != Action.SELECT_FILTER) {
            menuInflater.inflate(R.menu.categories, menu)
            menuInflater.inflate(R.menu.sync, menu)
            val exportMenu = menu.findItem(R.id.EXPORT_COMMAND)
            exportMenu.setEnabledAndVisible(action == Action.MANAGE)
            exportMenu.title = getString(R.string.export_to_format, "QIF")
            menu.findItem(R.id.TOGGLE_PARENT_CATEGORY_SELECTION_ON_TAP).setEnabledAndVisible(
                action == Action.SELECT_MAPPING
            )
            menu.findItem(R.id.TYPE_FILTER_COMMAND).isChecked = viewModel.typeFilter != null
        }
        menuInflater.inflate(R.menu.search, menu)
        configureSearch(this, menu, callback = ::onQueryTextChange)
        super.onCreateOptionsMenu(menu)
        return true
    }

    private fun onQueryTextChange(newText: String): Boolean {
        viewModel.filter = newText
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        sortDelegate.onPrepareOptionsMenu(menu)
        if (intent.asAction != Action.SELECT_FILTER) {
            menu.findItem(R.id.TOGGLE_PARENT_CATEGORY_SELECTION_ON_TAP)?.let {
                it.isChecked = parentSelectionOnTap.value
            }
            menu.findItem(R.id.TYPE_FILTER_COMMAND)?.let {
                checkMenuIcon(it, R.drawable.ic_filter)
            }
        }
        menu.prepareSearch(viewModel.filter)
        menu.prepareSync(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (sortDelegate.onOptionsItemSelected(item)) {
            invalidateOptionsMenu()
            viewModel.setSortOrder(sortDelegate.currentSortOrder)
            true
        } else if (item.itemId == Menu.NONE) {
            when (item.groupId) {
                R.id.SYNC_COMMAND_EXPORT -> viewModel.syncCatsExport(item.title.toString())
                R.id.SYNC_COMMAND_IMPORT -> viewModel.syncCatsImport(item.title.toString())
            }
            true
        } else super.onOptionsItemSelected(item)

    override val fabDescription: Int?
        get() {
            val action = intent.asAction
            return if (action == Action.SELECT_MAPPING || action == Action.MANAGE)
                R.string.menu_create_main_cat else null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val action = intent.asAction
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        floatingActionButton = binding.fab.CREATECOMMAND
        setContentView(binding.root)
        setupToolbar(true)
        injector.inject(viewModel)
        val (helpVariant, title) = when (action) {
            Action.MANAGE ->
                HELP_VARIANT_MANGE to R.string.pref_manage_categories_title

            Action.SELECT_FILTER ->
                HELP_VARIANT_SELECT_FILTER to R.string.search_category

            Action.SELECT_MAPPING ->
                HELP_VARIANT_SELECT_MAPPING to R.string.select_category
        }
        setHelpVariant(helpVariant, true)
        if (title != 0) supportActionBar!!.setTitle(title)
        floatingActionButton.isVisible = action != Action.SELECT_FILTER
        sortDelegate = SortDelegate(
            defaultSortOrder = viewModel.defaultSort,
            prefKey = PrefKey.SORT_ORDER_CATEGORIES,
            options = arrayOf(Sort.LABEL, Sort.USAGES, Sort.LAST_USED),
            prefHandler = prefHandler,
            collate = collate
        )
        parentSelectionOnTap.value = prefHandler.getBoolean(
            PrefKey.PARENT_CATEGORY_SELECTION_ON_TAP,
            false
        )
        viewModel.setSortOrder(sortDelegate.currentSortOrder)
        observeDeleteResult()
        observeMoveResult()
        observeImportResult()
        observeExportResult()
        observeSyncResult()
        observeMergeResult()
        val preSelected = this.preSelected
        supportFragmentManager.setFragmentResultListener(SetupCategoriesConfirmDialogFragment.IMPORT_OK, this) { _,_ ->
            showSnackBarIndefinite(R.string.menu_categories_setup_default)
            viewModel.importCats()
        }
        binding.composeView.setContent {
            val selectionState = rememberMutableStateListOf(preSelected ?: emptyList())
            AppTheme {
                choiceMode = when (action) {
                    Action.SELECT_MAPPING -> {
                        val selectionStateForMapping: MutableState<Category?> = remember {
                            mutableStateOf(null)
                        }
                        LaunchedEffect(selectionStateForMapping.value) {
                            selectionStateForMapping.value?.let {
                                if (it.level > 2) {
                                    contribFeatureRequested(
                                        ContribFeature.CATEGORY_TREE,
                                        R.id.SELECT_COMMAND to it
                                    )
                                } else {
                                    doSingleSelection(it)
                                }
                            }
                        }
                        ChoiceMode.SingleChoiceMode(
                            selectionStateForMapping,
                            parentSelectionOnTap.value
                        )
                    }

                    Action.MANAGE, Action.SELECT_FILTER -> {
                        ChoiceMode.MultiChoiceMode(selectionState, true)
                    }
                }
                when (viewModel.dialogState) {
                    is CategoryViewModel.Edit -> CategoryEdit(
                        dialogState = viewModel.dialogState as CategoryViewModel.Edit,
                        onDismissRequest = { viewModel.dialogState = CategoryViewModel.NoShow },
                        onSave = viewModel::saveCategory
                    )

                    is CategoryViewModel.Merge -> CategoryMerge(
                        dialogState = viewModel.dialogState as CategoryViewModel.Merge,
                        onDismissRequest = { viewModel.dialogState = CategoryViewModel.NoShow },
                        onMerge = viewModel::mergeCategories
                    )

                    else -> {}
                }
                viewModel.categoryTree.collectAsState(initial = Category.LOADING).value.let { state ->
                    LaunchedEffect(state) {
                        finishActionMode()
                    }
                    val typeFlags = viewModel.typeFilterLiveData.observeAsState(null).value
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (typeFlags != null) {
                            TypeConfiguration(
                                modifier = Modifier.align(CenterHorizontally),
                                typeFlags = typeFlags,
                                onCheckedChange = { viewModel.typeFilter = it }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (state) {
                                LoadingState.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .align(Alignment.Center)
                                    )
                                }

                                is LoadingState.Empty -> {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalAlignment = CenterHorizontally
                                    ) {
                                        Text(text = stringResource(id = R.string.no_categories))
                                        if (!state.hasUnfiltered) {
                                            Button(onClick = { checkImportableCategories() }) {
                                                Column(horizontalAlignment = CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                                        contentDescription = null
                                                    )
                                                    Text(text = stringResource(id = R.string.menu_categories_setup_default))
                                                }
                                            }
                                        }
                                    }
                                }

                                is LoadingState.Data -> {
                                    LaunchedEffect(selectionState.size) {
                                        if (selectionState.isNotEmpty()) {
                                            startActionMode(selectionState)
                                            updateActionModeTitle(selectionState.size)
                                        } else {
                                            finishActionMode()
                                        }
                                    }
                                    val preExpanded = remember {
                                        if (preSelected?.isEmpty() == false)
                                            state.data.getExpandedForSelected(preSelected) else emptyList()
                                    }
                                    val nestedScrollInterop = rememberNestedScrollInteropConnection()
                                    Category(
                                        modifier = Modifier.nestedScroll(nestedScrollInterop),
                                        category = if (action == Action.SELECT_FILTER)
                                            state.data.copy(children = buildList {
                                                add(
                                                    Category(
                                                        id = NULL_ITEM_ID,
                                                        label = stringResource(id = R.string.unmapped),
                                                        level = 1
                                                    )
                                                )
                                                addAll(state.data.children)
                                            })
                                        else state.data,
                                        expansionMode = ExpansionMode.DefaultCollapsed(
                                            rememberMutableStateListOf(preExpanded)
                                        ),
                                        menuGenerator = remember {
                                            { cat ->
                                                if (action == Action.SELECT_FILTER) null else Menu(
                                                    listOfNotNull(
                                                        if ((choiceMode as? ChoiceMode.SingleChoiceMode)?.selectParentOnClick == false) {
                                                            select("SELECT_CATEGORY") {
                                                                doSingleSelection(cat)
                                                            }
                                                        } else null,
                                                        edit("EDIT_CATEGORY") { editCat(cat) },
                                                        delete("DELETE_CATEGORY") {
                                                            when {
                                                                cat.flatten().map { it.id }
                                                                    .contains(protectionInfo?.id) -> {
                                                                    showSnackBar(
                                                                        resources.getQuantityString(
                                                                            if (protectionInfo!!.isTemplate) R.plurals.not_deletable_mapped_templates else R.plurals.not_deletable_mapped_transactions,
                                                                            1,
                                                                            1
                                                                        )
                                                                    )
                                                                }

                                                                checkDefaultTransferCategory(
                                                                    listOf(cat)
                                                                ) -> {
                                                                    viewModel.deleteCategories(
                                                                        listOf(cat)
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        MenuEntry(
                                                            icon = Icons.Filled.Add,
                                                            label = R.string.subcategory,
                                                            command = "CREATE_SUBCATEGORY"
                                                        ) {
                                                            if (cat.level > 1) {
                                                                contribFeatureRequested(
                                                                    ContribFeature.CATEGORY_TREE,
                                                                    R.id.CREATE_SUB_COMMAND to cat
                                                                )
                                                            } else {
                                                                createCat(cat)
                                                            }
                                                        },
                                                        MenuEntry(
                                                            icon = myiconpack.ArrowsAlt,
                                                            label = R.string.menu_move,
                                                            command = "MOVE_CATEGORY"
                                                        ) { showMoveTargetDialog(cat) }
                                                    )
                                                )
                                            }
                                        },
                                        choiceMode = choiceMode,
                                        contentPadding = WindowInsets.navigationBars
                                            .add(WindowInsets(bottom = dimensionResource(R.dimen.fab_related_bottom_padding)))
                                            .asPaddingValues()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @return true if default transfer category is not contained in any of the passed in category
     * trees
     */
    private fun checkDefaultTransferCategory(categories: Collection<Category>): Boolean {
        val defaultTransferCategory = prefHandler.defaultTransferCategory
        return categories.firstNotNullOfOrNull { cat ->
            cat.flatten().find { it.id == defaultTransferCategory }
        }?.also {
            showSnackBar(
                getString(
                    R.string.warning_delete_default_transfer_category,
                    it.path
                )
            )
        } == null
    }

    private fun doSingleSelection(category: Category) {
        val intent = Intent().apply {
            putExtra(KEY_ACCOUNTID, intent.getLongExtra(KEY_ACCOUNTID, 0))
            putExtra(KEY_ROWID, category.id)
            putExtra(KEY_LABEL, category.path)
            putExtra(KEY_ICON, category.icon)
            putExtra(KEY_TYPE, category.typeFlags)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    fun doMultiSelection() {
        val selected = (choiceMode as ChoiceMode.MultiChoiceMode).selectionState
        if (selected.size == 1 || !selected.contains(NULL_ITEM_ID)) {
            (viewModel.categoryTree.value as? LoadingState.Data)?.also { data ->
                val label = data.data.flatten()
                    .filter { selected.contains(it.id) }
                    .joinToString(separator = ",") { it.label }
                setResult(RESULT_FIRST_USER, Intent().apply {
                    putExtra(KEY_ACCOUNTID, intent.getLongExtra(KEY_ACCOUNTID, 0))
                    putExtra(KEY_ROWID, selected.toLongArray())
                    putExtra(KEY_LABEL, label)
                })
                finish()
            } ?: run {
                CrashHandler.report(
                    IllegalStateException(
                        "called doMultiSelection without data, found ${viewModel.categoryTree.value::class.simpleName}"
                    )
                )
            }
        } else {
            showSnackBar(R.string.unmapped_filter_only_single)
        }
    }

    private fun showMoveTargetDialog(category: Category) {
        SelectCategoryMoveTargetDialogFragment.newInstance(category)
            .show(supportFragmentManager, "SELECT_TARGET")
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun updateActionModeTitle(size: Int) {
        actionMode?.title = size.toString()
    }

    private fun selectedCategories(selectionState: SnapshotStateList<Long>) =
        (viewModel.categoryTree.value as? LoadingState.Data)?.let { state ->
            state.data.flatten().filter { selectionState.contains(it.id) }
        }

    private fun startActionMode(selectionState: SnapshotStateList<Long>) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu
                ): Boolean {
                    val action = intent.asAction
                    if (action == Action.MANAGE) {
                        menu.add(
                            Menu.NONE,
                            R.id.DELETE_COMMAND,
                            0,
                            R.string.menu_delete
                        ).setIcon(R.drawable.ic_menu_delete)
                        menu.add(
                            Menu.NONE,
                            R.id.MERGE_COMMAND,
                            0,
                            R.string.menu_merge
                        ).setIcon(R.drawable.ic_menu_split_transaction)
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
                ): Boolean {
                    menu.findItem(R.id.MERGE_COMMAND)?.isVisible =
                        selectedCategories(selectionState)?.let { list ->
                            list.size >= 2 && list.all { it.typeFlags == list.first().typeFlags }
                        } == true
                    return true
                }

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem
                ): Boolean = when (item.itemId) {
                    R.id.DELETE_COMMAND -> {
                        selectedCategories(selectionState)?.let { list ->
                            if (checkDefaultTransferCategory(list)) {
                                viewModel.deleteCategories(list)
                            }
                        }
                        true
                    }

                    R.id.SELECT_COMMAND -> {
                        doMultiSelection()
                        true
                    }

                    R.id.MERGE_COMMAND -> {
                        selectedCategories(selectionState)?.let { list ->
                            viewModel.dialogState = CategoryViewModel.Merge(list)
                        }
                        true
                    }

                    else -> false
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                    selectionState.clear()
                }

            })
        } else actionMode?.invalidate()
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
            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION || event == DISMISS_EVENT_TIMEOUT)
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
                    result?.onSuccess { deleteResult ->
                        when (deleteResult) {
                            is OperationComplete -> {
                                finishActionMode()
                                //noinspection BuildListAdds
                                val messages = buildList {
                                    mapToMessage(deleteResult.deleted, R.plurals.delete_success)
                                    mapToMessage(
                                        deleteResult.mappedToTransactions,
                                        R.plurals.not_deletable_mapped_transactions
                                    )
                                    mapToMessage(
                                        deleteResult.mappedToTemplates,
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
                                    if (deleteResult.hasDescendants > 0) {
                                        mapToMessage(
                                            deleteResult.hasDescendants,
                                            R.plurals.warning_delete_main_category
                                        )
                                    }
                                    if (deleteResult.mappedToBudgets > 0) {
                                        add(getString(R.string.warning_delete_category_with_budget))
                                    }
                                    add(getString(R.string.continue_confirmation))
                                }
                                val labels = deleteResult.categories.joinToString { it.label }
                                MessageDialogFragment.newInstance(
                                    getString(R.string.dialog_title_warning_delete_category) +
                                            " ($labels)",
                                    messages.joinToString(" "),
                                    MessageDialogFragment.Button(
                                        R.string.response_yes,
                                        R.id.DELETE_COMMAND_DO,
                                        deleteResult.categories.map { it.id }.toTypedArray()
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
                    result?.onSuccess { pair ->
                        updateDismissibleSnackBar(
                            getString(
                                R.string.export_sdcard_success,
                                pair.second
                            )
                        )
                        if (prefHandler.getBoolean(PrefKey.PERFORM_SHARE, false)) {
                            baseViewModel.share(
                                this@ManageCategories, listOf(pair.first),
                                prefHandler.requireString(PrefKey.SHARE_TARGET, "").trim(),
                                "text/qif"
                            )
                        }
                    }?.onFailure {
                        updateDismissibleSnackBar(it.safeMessage)
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

    private fun observeSyncResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncResult.collect {
                    showSnackBar(it, callback = dismissCallback)
                }
            }
        }
    }

    private fun observeMergeResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mergeResult.collect {
                    finishActionMode()
                    viewModel.messageShown()
                }
            }
        }
    }

    override val fabActionName = "CREATE_CATEGORY"

    override fun onFabClicked() {
        super.onFabClicked()
        createCat(null)
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) {
            true
        } else when (command) {
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
                checkImportableCategories()
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

            R.id.TOGGLE_PARENT_CATEGORY_SELECTION_ON_TAP -> {
                val value = tag as Boolean
                parentSelectionOnTap.value = value
                prefHandler.putBoolean(PrefKey.PARENT_CATEGORY_SELECTION_ON_TAP, value)
                true
            }

            R.id.TYPE_FILTER_COMMAND -> {
                viewModel.toggleTypeFilterIsShown()
                invalidateOptionsMenu()
                true
            }

            else -> false
        }

    private val protectionInfo: ProtectionInfo?
        get() = intent.getParcelableExtra(KEY_PROTECTION_INFO)

    private fun checkImportableCategories() {
        viewModel.checkImportableCategories().observe(this) {
            SetupCategoriesConfirmDialogFragment.newInstance(it).show(
                supportFragmentManager, "CONFIRM"
            )
        }
    }

    private fun exportCats(encoding: String) {
        showDismissibleSnackBar(getString(R.string.export_to_format, "QIF"), dismissCallback)
        viewModel.exportCats(encoding)
    }

    /**
     * presents AlertDialog for adding a new category
     * if label is already used, shows an error
     */
    private fun createCat(parent: Category?) {
        viewModel.dialogState = CategoryViewModel.Edit(
            parent = parent,
            category = if (parent == null) Category(
                typeFlags = viewModel.typeFilter ?: FLAG_NEUTRAL
            ) else null
        )
    }

    /**
     * presents AlertDialog for editing an existing category
     */
    private fun editCat(category: Category) {
        viewModel.dialogState =
            CategoryViewModel.Edit(category = category)
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature == ContribFeature.CATEGORY_TREE) {
            val (command, category) = tag as Pair<Int, Category>
            if (command == R.id.CREATE_SUB_COMMAND) {
                createCat(category)
            } else if (command == R.id.SELECT_COMMAND) {
                doSingleSelection(category)
            }
        }
    }

    companion object {
        const val KEY_PROTECTION_INFO = "protection_info"
    }

    @Parcelize
    data class ProtectionInfo(val id: Long, val isTemplate: Boolean) : Parcelable
}