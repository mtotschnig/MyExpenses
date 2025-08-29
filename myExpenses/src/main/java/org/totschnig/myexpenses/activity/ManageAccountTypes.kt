package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.SortableItem
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.ButtonDefinition
import org.totschnig.myexpenses.compose.DialogFrame
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.viewmodel.AccountTypeViewModel
import org.totschnig.myexpenses.viewmodel.AccountTypesUiState

class ManageAccountTypes : ProtectedFragmentActivity(),
    SortUtilityDialogFragment.OnConfirmListener  {

    val viewModel: AccountTypeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                ManageAccountTypesScreen(
                    uiState = uiState,
                    onClose = { finish() },
                    onAdd = viewModel::onAdd,
                    onEdit = viewModel::onEdit,
                    onDelete = viewModel::onDelete,
                    onDialogDismiss = viewModel::onDialogDismiss,
                    onSave = viewModel::onSave,
                    onSort = { isAsset ->
                        SortUtilityDialogFragment.newInstance(
                            ArrayList(
                                uiState.accountTypes.filter { it.isAsset  == isAsset }.map {
                                    SortableItem(it.id, it.localizedName(this))
                                }
                            ))
                            .show(supportFragmentManager, "SORT_TYPES")
                    }
                )
            }
        }
    }

    override fun onSortOrderConfirmed(sortedIds: LongArray) {

        viewModel.onSortOrderConfirmed(sortedIds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageAccountTypesScreen(
    uiState: AccountTypesUiState,
    onClose: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (AccountType) -> Unit = {},
    onDelete: (AccountType) -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onSave: (name: String, isAsset: Boolean, supportsReconciliation: Boolean) -> Unit = { _, _, _ -> },
    onSort: (Boolean) -> Unit = {}
) {
    val scrollBehavior = pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_types)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = androidx.appcompat.R.string.abc_action_bar_up_description)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.new_account_type)
                )
            }
        }
    ) { paddingValues ->
        val horizontalPadding = dimensionResource(id = R.dimen.padding_main_screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {

                val layoutDirection = LocalLayoutDirection.current

                val totalPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection) + horizontalPadding,
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection) + horizontalPadding,
                    bottom = paddingValues.calculateBottomPadding() + dimensionResource(R.dimen.fab_related_bottom_padding)
                )
                AccountTypeList(
                    paddingValues = totalPadding,
                    accountTypes = uiState.accountTypes,
                    onEditClick = { onEdit(it) },
                    onDeleteClick = { onDelete(it) },
                    onSortClick = onSort
                )
            }

            if (uiState.editingAccountType != null) {
                AddEditAccountTypeDialog(
                    allTypes = uiState.accountTypes,
                    editingAccountType = uiState.editingAccountType,
                    onDismiss = onDialogDismiss,
                    onSave = { name, isAsset, supportsReconciliation ->
                        onSave(name, isAsset, supportsReconciliation)
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountTypeList(
    accountTypes: List<AccountType>,
    onEditClick: (AccountType) -> Unit,
    onDeleteClick: (AccountType) -> Unit,
    onSortClick: (Boolean) -> Unit,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current

    if (accountTypes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data))
        }
        return
    }
    val groups = accountTypes.groupBy { it.isAsset }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier.fillMaxSize()
    ) {
        groups[true]?.let { assetTypes ->
            section(
                context.getString(R.string.balance_sheet_section_assets),
                assetTypes,
                onEditClick,
                onDeleteClick,
                onSort = { onSortClick(true) }
            )
        }
        item {
            HorizontalDivider()
        }
        groups[false]?.let { liabilityTypes ->
            section(
                context.getString(R.string.balance_sheet_section_liabilities),
                liabilityTypes,
                onEditClick,
                onDeleteClick,
                onSort = { onSortClick(false) }
            )
        }
    }
}

private fun LazyListScope.section(
    title: String,
    list: List<AccountType>,
    onEditClick: (AccountType) -> Unit,
    onDeleteClick: (AccountType) -> Unit,
    onSort: () -> Unit
) {
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onSort) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.menu_sort))
            }
        }
    }
    items(list, key = { it.id }) { accountType ->
        AccountTypeItem(
            modifier = Modifier.animateItem(),
            accountType = accountType,
            onEditClick = { onEditClick(accountType) },
            onDeleteClick = { onDeleteClick(accountType) }
        )
    }
}

@Composable
private fun AccountTypeItem(
    modifier: Modifier,
    accountType: AccountType,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    val showMenu = rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .conditional(!accountType.isPredefined) {
                clickable {
                    showMenu.value = true
                }
            }
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = accountType.localizedName(context) +
                    ((accountType.count ?: 0).takeIf { it > 0 }?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!accountType.isPredefined) {
            val menu = Menu(
                buildList {
                    add(MenuEntry.edit("EDIT_FLAG", onEditClick))
                    if ((accountType.count ?: 0) == 0) {
                        add(MenuEntry.delete("DELETE_FLAG", onDeleteClick))
                    }
                }
            )
            HierarchicalMenu(showMenu, menu)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAccountTypeDialog(
    editingAccountType: AccountType,
    onDismiss: () -> Unit = {},
    onSave: (name: String, isAsset: Boolean, supportsReconciliation: Boolean) -> Unit =
        { _, _, _ -> },
    allTypes: List<AccountType> = emptyList()
) {

    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf(editingAccountType.name) }

    val title =
        stringResource(if (editingAccountType.id == 0L) R.string.new_account_type else R.string.edit_account_type)

    val options =
        listOf(R.string.balance_sheet_section_assets, R.string.balance_sheet_section_liabilities)

    var selectedIndex by rememberSaveable {
        mutableIntStateOf(if (editingAccountType.isAsset) 0 else 1)
    }

    var supportsReconciliation by rememberSaveable {
        mutableStateOf(
            editingAccountType.supportsReconciliation
        )
    }

    val nameAlreadyExists = remember {
        derivedStateOf {
            AccountType.isReservedName(name) ||
                    allTypes.any {
                        it.id != editingAccountType.id &&
                                (it.name == name || it.localizedName(context) == name)
                    }
        }
    }

    DialogFrame(
        title = title,
        onDismissRequest = onDismiss,
        positiveButton = ButtonDefinition(
            text = R.string.menu_save,
            enabled = name.isNotBlank() && !nameAlreadyExists.value
        ) {
            onSave(name, selectedIndex == 0, supportsReconciliation)
        }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameAlreadyExists.value,
            supportingText = {
                if (nameAlreadyExists.value) {
                    Text(
                        text = stringResource(R.string.already_exists),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex
                ) {
                    Text(stringResource(label))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    supportsReconciliation = !supportsReconciliation
                } // Make row clickable
                .padding(vertical = 4.dp), // Add some vertical padding for better touch target
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = supportsReconciliation,
                onCheckedChange = { supportsReconciliation = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.supports_reconciliation))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListPreview() {
    ManageAccountTypesScreen(uiState = AccountTypesUiState())
}

@Composable
@Preview(showBackground = true, group = "Dialog")
fun EditPreview() {
    AddEditAccountTypeDialog(
        AccountType(name = "")
    )
}