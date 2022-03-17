package org.totschnig.myexpenses.activity

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SelectColorField
import eltos.simpledialogfragment.form.SelectIconField
import eltos.simpledialogfragment.form.SimpleFormDialog
import org.totschnig.myexpenses.*
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.compose.toggle
import org.totschnig.myexpenses.databinding.ActivityCategoryComposeBinding
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.Sort.Companion.preferredOrderByForCategories
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.configureSearch
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.prepareSearch
import org.totschnig.myexpenses.viewmodel.CategoryViewModel

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
        binding.composeView.setContent {
            AppTheme(this) {
                val selectionState = rememberMutableStateListOf<Long>()
                Category(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.general_padding)),
                    category = viewModel.categoryTree.collectAsState(initial = Category.EMPTY).value,
                    expansionState = rememberMutableStateListOf(),
                    selectionState = selectionState,
                    onEdit = { editCat(it) },
                    onDelete = { },
                    onToggleSelection = {
                        selectionState.toggle(it)
                        if (selectionState.size == 0) {
                            selectionState.clear()
                            actionMode?.finish()
                        } else {
                            if (actionMode == null) {
                                actionMode = startSupportActionMode(object : ActionMode.Callback {
                                    override fun onCreateActionMode(
                                        mode: ActionMode,
                                        menu: Menu
                                    ): Boolean {
                                        menu.add("Delete")
                                        return true
                                    }

                                    override fun onPrepareActionMode(
                                        mode: ActionMode?,
                                        menu: Menu?
                                    ): Boolean = true

                                    override fun onActionItemClicked(
                                        mode: ActionMode?,
                                        item: MenuItem?
                                    ): Boolean = false

                                    override fun onDestroyActionMode(mode: ActionMode?) {
                                        actionMode = null
                                    }

                                })
                            }
                            actionMode?.title = "${selectionState.size}"
                        }
                    }
                )
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