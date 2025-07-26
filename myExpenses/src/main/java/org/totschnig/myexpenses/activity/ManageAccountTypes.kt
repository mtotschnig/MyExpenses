package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.viewmodel.AccountTypeViewModel
import org.totschnig.myexpenses.viewmodel.AccountTypesUiState

class ManageAccountTypes : ProtectedFragmentActivity() {

    val viewModel: AccountTypeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {
                ManageAccountTypesScreen(
                    uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
                    onClose = { finish() },
                    onAdd = viewModel::onAdd,
                    onEdit = viewModel::onEdit,
                    onDelete = viewModel::deleteAccountType,
                    onDialogDismiss = viewModel::onDialogDismiss,
                    onSave = viewModel::onSave
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountTypesScreen(
    uiState: AccountTypesUiState,
    onClose: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (AccountType) -> Unit = {},
    onDelete: (AccountType) -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onSave: (name: String, isAsset: Boolean, supportsReconciliation: Boolean) -> Unit = { _, _, _ -> }
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_account_types)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = androidx.appcompat.R.string.abc_action_bar_up_description)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add Account Type")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AccountTypeList(
                    accountTypes = uiState.accountTypes,
                    onEditClick = { onEdit(it) },
                    onDeleteClick = { onDelete(it) }
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
fun AccountTypeList(
    accountTypes: List<AccountType>,
    onEditClick: (AccountType) -> Unit,
    onDeleteClick: (AccountType) -> Unit
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups[true]?.let { assetTypes ->
            section(
                context.getString(R.string.balance_sheet_section_assets),
                assetTypes.sortedBy { it.localizedName(context) },
                onEditClick,
                onDeleteClick
            )
        }
        groups[false]?.let { liabilityTypes ->
            section(
                context.getString(R.string.balance_sheet_section_liabilities),
                liabilityTypes.sortedBy { it.localizedName(context) },
                onEditClick,
                onDeleteClick
            )
        }
    }
}

fun LazyListScope.section(
    title: String,
    list: List<AccountType>,
    onEditClick: (AccountType) -> Unit,
    onDeleteClick: (AccountType) -> Unit
) {
    item {
        Text(title, style = MaterialTheme.typography.titleMedium)
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
fun AccountTypeItem(
    accountType: AccountType,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val isUsedBy = accountType.count ?: 0
        Text(
            text = accountType.localizedName(context) +
                    (isUsedBy.takeIf { it > 0 }?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (!accountType.isPredefined) {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
            if (isUsedBy == 0) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountTypeDialog(
    editingAccountType: AccountType,
    onDismiss: () -> Unit = {},
    onSave: (name: String, isAsset: Boolean, supportsReconciliation: Boolean) -> Unit =
        { _, _, _ -> },
    allTypes: List<AccountType> = emptyList()
) {

    val context = LocalContext.current

    var name by remember { mutableStateOf(editingAccountType.name) }

    val title =
        stringResource(if (editingAccountType.id == 0L) R.string.new_account_type else R.string.edit_account_type)

    val options =
        listOf(R.string.balance_sheet_section_assets, R.string.balance_sheet_section_liabilities)

    var selectedIndex by remember {
        mutableIntStateOf(if (editingAccountType.isAsset) 0 else 1)
    }

    var supportsReconciliation by remember {
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

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Type Name") },
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
                    Text("Supports Reconciliation")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        enabled = name.isNotBlank() && !nameAlreadyExists.value,
                        onClick = {
                            onSave(name, selectedIndex == 0, supportsReconciliation)
                        }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
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