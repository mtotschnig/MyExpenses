package org.totschnig.myexpenses.activity

// For ViewModel (you'd use your actual ViewModel injection)
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.ButtonDefinition
import org.totschnig.myexpenses.compose.DialogFrame
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.IconSelectorDialog
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.viewmodel.AccountFlagsUiState
import org.totschnig.myexpenses.viewmodel.AccountFlagsViewModel

private const val WEIGHT_ICON = 1f
private const val WEIGHT_LABEL = 7f
private const val WEIGHT_VISIBLE = 2f


class ManageAccountFlags : ProtectedFragmentActivity() {

    val viewModel: AccountFlagsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {
                ManageFlagsScreen(
                    uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
                    onClose = { finish() },
                    onAdd = viewModel::onAdd,
                    onEdit = viewModel::onEdit,
                    onDelete = viewModel::onDelete,
                    onDialogDismiss = viewModel::onDialogDismiss,
                    onSave = viewModel::onSave,
                    onToggleVisibility = viewModel::onToggleVisibility
                )
            }
        }
    }
}


// Drag and Drop support using a library or custom implementation can be complex.
// This example will use a simplified conceptual drag indication.
// For full drag and drop, libraries like "androidx.compose.foundation:foundation-draganddrop" (experimental)
// or custom implementations with pointerInput are needed.
// This example simulates the idea with long press.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageFlagsScreen(
    uiState: AccountFlagsUiState,
    onClose: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (AccountFlag) -> Unit = {},
    onDelete: (AccountFlag) -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onSave: (String, String?) -> Unit,
    onToggleVisibility: (Long, Boolean) -> Unit
) {
    Scaffold(
        modifier = Modifier.padding(
            horizontal = dimensionResource(
                id = R.dimen.padding_main_screen
            )
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_account_flags)) },
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
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.new_account_flag)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {

                val layoutDirection = LocalLayoutDirection.current

                val totalPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection) + 0.dp, // Or add horizontal FAB padding if needed
                    top = paddingValues.calculateTopPadding() + 0.dp,
                    end = paddingValues.calculateEndPadding(layoutDirection) + 0.dp,   // Or add horizontal FAB padding if needed
                    bottom = paddingValues.calculateBottomPadding() + dimensionResource(R.dimen.fab_related_bottom_padding)
                )
                AccountFlagList(
                    paddingValues = totalPadding,
                    list = uiState.accountFlags,
                    onEditClick = { onEdit(it) },
                    onDeleteClick = { onDelete(it) },
                    onToggleVisibility = { id, visible ->
                        onToggleVisibility(id, visible)
                    }
                )
            }

            if (uiState.editingAccountFlag != null) {
                AddEditAccountFlagDialog(
                    allFlags = uiState.accountFlags,
                    editingAccountFlag = uiState.editingAccountFlag,
                    onDismiss = onDialogDismiss,
                    onSave = onSave
                )
            }
        }
    }
}

@Composable
fun AccountFlagList(
    list: List<AccountFlag>,
    onEditClick: (AccountFlag) -> Unit,
    onDeleteClick: (AccountFlag) -> Unit,
    onToggleVisibility: (Long, Boolean) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            Spacer(modifier = Modifier.weight(WEIGHT_ICON))
            Text(
                modifier = Modifier.weight(WEIGHT_LABEL),
                text = "${stringResource(R.string.label)} (${stringResource(R.string.count)})",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                modifier = Modifier.weight(WEIGHT_VISIBLE),
                text = stringResource(R.string.visible),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {

            }
            items(list, key = { it.id }) { flag ->
                AccountFlagItem(
                    modifier = Modifier.animateItem(),
                    flag = flag,
                    onToggleVisibility = {
                        onToggleVisibility(flag.id, it)
                    },
                    onEditClick = {
                        onEditClick(flag)
                    },
                    onDeleteClick = {
                        onDeleteClick(flag)
                    }
                )
            }
        }
    }
}

@Composable
fun AccountFlagItem(
    modifier: Modifier = Modifier,
    flag: AccountFlag,
    onToggleVisibility: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val showMenu = remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                showMenu.value = true
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(WEIGHT_ICON)) {
            if (flag.icon != null) {
                org.totschnig.myexpenses.compose.Icon(
                    modifier = Modifier.align(Alignment.Center),
                    icon = flag.icon
                )
            }
        }
        val context = LocalContext.current
        Text(
            text = flag.localizedLabel(context) +
                    ((flag.count ?: 0).takeIf { it > 0 }?.let { " ($it)" } ?: ""),
            modifier = Modifier
                .weight(WEIGHT_LABEL),
            style = MaterialTheme.typography.bodyLarge
        )
        Checkbox(
            modifier = Modifier.weight(WEIGHT_VISIBLE),
            checked = flag.isVisible,
            onCheckedChange = { onToggleVisibility(it) }
        )
        if (flag.id > 0) {
            val menu = Menu(
                buildList {
                    add(MenuEntry.edit("EDIT_FLAG", onEditClick))
                    if ((flag.count ?: 0) == 0) {
                        add(MenuEntry.delete("DELETE_FLAG", onDeleteClick))
                    }
                }
            )
            HierarchicalMenu(showMenu, menu)
        }
    }
}

@Composable
fun AddEditAccountFlagDialog(
    editingAccountFlag: AccountFlag,
    onDismiss: () -> Unit = {},
    onSave: (name: String, icon: String?) -> Unit =
        { _, _ -> },
    allFlags: List<AccountFlag> = emptyList()
) {
    val context = LocalContext.current

    var label by remember { mutableStateOf(editingAccountFlag.localizedLabel(context)) }
    val icon = rememberSaveable { mutableStateOf(editingAccountFlag.icon) }

    val showIconSelection = rememberSaveable { mutableStateOf(false) }

    val title =
        stringResource(if (editingAccountFlag.id == 0L) R.string.new_account_flag else R.string.edit_account_flag)

    val labelAlreadyExists = remember {
        derivedStateOf {
            AccountFlag.isReservedName(label) ||
                    allFlags.any {
                        it.id != editingAccountFlag.id &&
                                (it.label == label || it.localizedLabel(context) == label)
                    }
        }
    }

    DialogFrame(
        title = title,
        onDismissRequest = onDismiss,
        positiveButton = ButtonDefinition(
            text = R.string.menu_save,
            enabled = label.isNotBlank() && !labelAlreadyExists.value
        ) {
            onSave(label, icon.value)
        }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(R.string.label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = labelAlreadyExists.value,
            supportingText = {
                if (labelAlreadyExists.value) {
                    Text(
                        text = stringResource(R.string.already_exists),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = R.string.icon))
        Button(onClick = { showIconSelection.value = true }) {
            icon.value?.let {
                org.totschnig.myexpenses.compose.Icon(it)
            } ?: Text(stringResource(id = R.string.select))
        }
        IconSelectorDialog(showIconSelection, icon)
    }
}
