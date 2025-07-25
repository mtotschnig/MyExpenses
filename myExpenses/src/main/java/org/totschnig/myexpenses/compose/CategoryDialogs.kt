package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.viewmodel.CategoryViewModel

@Composable
fun CategoryEdit(
    dialogState: CategoryViewModel.Edit,
    onDismissRequest: () -> Unit = {},
    onSave: (String, String?, Byte) -> Unit = { _, _, _ -> }
) {
    val fieldPadding = 12.dp
    val context = LocalContext.current
    var label by rememberSaveable { mutableStateOf(dialogState.category?.label ?: "") }
    var icon by rememberSaveable { mutableStateOf(dialogState.category?.icon) }
    var typeFlags by rememberSaveable { mutableStateOf(dialogState.category?.typeFlags ?: FLAG_NEUTRAL) }
    var shouldValidate by remember { mutableStateOf(false) }
    var showIconSelection by rememberSaveable { mutableStateOf(false) }

    val isError = if (shouldValidate) {
        when {
            dialogState.error -> context.getString(R.string.already_defined, label)
            label.isBlank() -> context.getString(R.string.required)
            else -> null
        }
    } else null

    DialogFrame(
        title = if (dialogState.isNew) {
            if (dialogState.parent == null) stringResource(R.string.menu_create_main_cat)
            else stringResource(R.string.menu_create_sub_cat) + " (${dialogState.parent.label})"
        } else stringResource(R.string.menu_edit_cat),
        onDismissRequest = onDismissRequest,
        cancelEnabled = !dialogState.saving,
        positiveButton = Triple(
            if (dialogState.isNew) R.string.menu_add else R.string.menu_save,
            !dialogState.saving && isError == null
        ) {
            shouldValidate = true
            if (label.isNotBlank()) {
                onSave(label, icon, typeFlags)
            }
        }
    ) {
        if (dialogState.category?.parentId == null && dialogState.parent == null) {
            TypeConfiguration(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                typeFlags = typeFlags,
                onCheckedChange = { typeFlags = it }
            )
            Spacer(modifier = Modifier.height(fieldPadding))
        }
        OutlinedTextField(
            modifier = Modifier.testTag(TEST_TAG_EDIT_TEXT),
            label = { Text(stringResource(id = R.string.label)) },
            value = label,
            isError = isError != null,
            onValueChange = { shouldValidate = false; label = it })
        Text(
            text = isError ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 0.dp)
        )

        Spacer(modifier = Modifier.height(fieldPadding))

        Text(stringResource(id = R.string.icon))
        Button(onClick = { showIconSelection = true }) {
            icon?.let {
                Icon(it)
            } ?: Text(stringResource(id = R.string.select))
        }
    }
    if (showIconSelection) {
        Dialog(
            properties = DialogProperties(
                usePlatformDefaultWidth = booleanResource(R.bool.isLarge),
                decorFitsSystemWindows = false
            ),
            onDismissRequest = { showIconSelection = false }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
                    IconSelector(
                        modifier = Modifier.weight(1f),
                        onIconSelected = {
                            icon = it
                            showIconSelection = false
                        }
                    )
                    ButtonRow(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                        onCancel = { showIconSelection = false },
                        positiveButton = if (icon != null)
                            Triple(R.string.remove, true) {
                                icon = null
                                showIconSelection = false
                            } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMerge(
    dialogState: CategoryViewModel.Merge,
    onDismissRequest: () -> Unit = {},
    onMerge: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val shape = if (expanded) RoundedCornerShape(8.dp).copy(
        bottomEnd = CornerSize(0.dp),
        bottomStart = CornerSize(0.dp)
    ) else RoundedCornerShape(8.dp)
    DialogFrame(
        title = stringResource(R.string.merge_categories_dialog_title),
        onDismissRequest = onDismissRequest,
        cancelEnabled = !dialogState.saving,
        positiveButton = Triple(
            R.string.menu_merge,
            !dialogState.saving
        ) { onMerge(selectedIndex) }
    ) {
        Text(
            text = stringResource(R.string.merge_categories_prompt),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ExposedDropdownMenuBox(
            modifier = Modifier.fillMaxWidth(),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = dialogState.categories[selectedIndex].path,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = shape,
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                dialogState.categories.forEachIndexed { index, cat ->
                    DropdownMenuItem(
                        text = { Text(cat.path) },
                        onClick = {
                            selectedIndex = index
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.subcategories_merged_recursively),
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            color = MaterialTheme.colorScheme.error,
            text = stringResource(id = R.string.cannot_be_undone),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DialogFrame(
    title: String,
    onDismissRequest: () -> Unit = {},
    cancelEnabled: Boolean,
    positiveButton: Triple<Int, Boolean, () -> Unit>?,
    content: @Composable ColumnScope.() -> Unit
) {
    val titleBottomPadding = 12.dp
    Dialog(
        onDismissRequest = { }
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Text(
                    modifier = Modifier.padding(bottom = titleBottomPadding),
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                content()
                ButtonRow(
                    onCancel = onDismissRequest,
                    positiveButton = positiveButton,
                    cancelEnabled = cancelEnabled
                )
            }
        }
    }
}

@Composable
private fun ButtonRow(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    positiveButton: Triple<Int, Boolean, () -> Unit>?,
    cancelEnabled: Boolean = true
) {
    val buttonRowTopPadding = 12.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = buttonRowTopPadding)
    ) {
        TextButton(
            modifier = Modifier.weight(1f),
            enabled = cancelEnabled,
            onClick = onCancel
        ) {
            Text(stringResource(id = android.R.string.cancel))
        }

        if (positiveButton != null) {
            TextButton(
                modifier = Modifier
                    .testTag(TEST_TAG_POSITIVE_BUTTON)
                    .weight(1f),
                enabled = positiveButton.second,
                onClick = positiveButton.third
            ) {
                Text(stringResource(id = positiveButton.first))
            }
        }
    }
}

@Preview(widthDp = 200)
@Composable
private fun PreviewDialog() {
    CategoryEdit(dialogState = CategoryViewModel.Edit())
}