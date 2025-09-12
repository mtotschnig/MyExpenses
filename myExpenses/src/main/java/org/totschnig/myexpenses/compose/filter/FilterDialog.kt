package org.totschnig.myexpenses.compose.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CharIcon
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.filter.AccountCriterion
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.CrStatusCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.DisplayInfo
import org.totschnig.myexpenses.provider.filter.MethodCriterion
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.OrCriterion
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.provider.filter.TransferCriterion
import org.totschnig.myexpenses.provider.filter.asSet
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount

const val COMPLEX_AND = 0
const val COMPLEX_OR = 1

const val TYPE_QUICK = 0
const val TYPE_COMPLEX = 1

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    account: BaseAccount,
    sumInfo: SumInfo,
    initialPreferredSearchType: Int = TYPE_COMPLEX,
    criterion: Criterion? = null,
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: (Int, Criterion?) -> Unit = {_, _ -> },
) {

    val initialSet = criterion.asSet
    val initialSelectedComplex = if (criterion is OrCriterion) COMPLEX_OR else COMPLEX_AND

    var selectedComplex by rememberSaveable {
        mutableIntStateOf(initialSelectedComplex)
    }

    var preferredSearchType by rememberSaveable {
        mutableIntStateOf(initialPreferredSearchType)
    }

    val isComplexSearch = preferredSearchType == TYPE_COMPLEX

    val criteriaSet: MutableState<Set<Criterion>> = rememberSaveable(
        saver = Saver(
            save = { ArrayList(it.value) },
            restore = { mutableStateOf(it.toSet()) }
        )
    ) {
        mutableStateOf(initialSet)
    }

    val isDirty by remember {
        derivedStateOf {
            initialSet != criteriaSet.value || initialSelectedComplex != selectedComplex
        }
    }

    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    val onResult: (SimpleCriterion<*>?, SimpleCriterion<*>?) -> Unit = remember(isComplexSearch) {
        { oldValue, newValue ->
            if (newValue != null) {
                if (isComplexSearch) {
                    oldValue?.also { current ->
                        criteriaSet.value = criteriaSet.value.map {
                            if (it == current)
                                if (it is NotCriterion) NotCriterion(newValue) else newValue
                            else it
                        }.toSet()
                    } ?: run { criteriaSet.value += newValue }
                } else {
                    onConfirmRequest(
                        TYPE_QUICK,
                        if (criteriaSet.value.isEmpty()) newValue else AndCriterion(criteriaSet.value + newValue)
                    )
                }
            }
        }
    }

    val isLarge = booleanResource(R.bool.isLarge)

    val onDismiss = {
        if (isDirty) {
            confirmDiscard = true
        } else {
            onDismissRequest()
        }
    }

    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = isLarge,
            decorFitsSystemWindows = false
        ),
        onDismissRequest = onDismiss
    ) {

        Surface(
            modifier = Modifier
                .testTag(TEST_TAG_DIALOG)
                .conditional(
                    isLarge,
                    ifTrue = { defaultMinSize(minHeight = 400.dp) },
                    ifFalse = { fillMaxSize() }
                )) {
            FilterHandler(account, "confirmFilterDialog", onResult) {

                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .conditional(isLarge && criteriaSet.value.isEmpty()) {
                            height(400.dp)
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val options = listOf(
                            stringResource(R.string.quick_search),
                            stringResource(R.string.complex_search)
                        )
                        ExposedDropdownMenuBox(
                            modifier = Modifier.align(Alignment.Center),
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }) {
                            BasicTextField(
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                value = options[preferredSearchType],
                                onValueChange = {},
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = LocalContentColor.current
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    it()
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )
                                }
                            }
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                options.forEachIndexed { index, s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        enabled = index == 1 || criteriaSet.value.isSimple(selectedComplex),
                                        onClick = {
                                            preferredSearchType = index
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                        ActionButton(
                            hintText = stringResource(android.R.string.cancel),
                            icon = Icons.Filled.Clear,
                            modifier = Modifier.align(if (isComplexSearch) Alignment.CenterStart else Alignment.CenterEnd),
                            onClick = onDismiss
                        )
                        if (isComplexSearch) {
                            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                                ActionButton(
                                    hintText = stringResource(R.string.clear_all_filters),
                                    icon = Icons.Filled.ClearAll,
                                    enabled = criteriaSet.value.isNotEmpty()
                                ) {
                                    confirmClear = true
                                }
                                ActionButton(
                                    hintText = stringResource(R.string.apply),
                                    icon = Icons.Filled.Done,
                                    enabled = isDirty
                                ) {
                                    onConfirmRequest(
                                        TYPE_COMPLEX,
                                        criteriaSet.value.wrap(selectedComplex)
                                    )
                                }
                            }
                        }
                    }

                    val filters: List<DisplayInfo> = listOfNotNull(
                        if (sumInfo.mappedCategories) CategoryCriterion else null,
                        AmountCriterion,
                        CommentCriterion,
                        if (account.isAggregate || account.type.supportsReconciliation)
                            CrStatusCriterion else null,
                        if (sumInfo.mappedPayees) PayeeCriterion else null,
                        if (sumInfo.mappedMethods) MethodCriterion else null,
                        DateCriterion,
                        if (sumInfo.hasTransfers) TransferCriterion else null,
                        if (sumInfo.hasTags) TagCriterion else null,
                        if (account.isAggregate) AccountCriterion else null
                    )
                    if (isComplexSearch) {
                        FlowRow {
                            filters.forEach { info ->
                                val accessibleButtonText =
                                    stringResource(R.string.add_filter) + ": " +
                                            stringResource(info.extendedTitle)
                                TextButton(
                                    modifier = Modifier.clearAndSetSemantics {
                                        contentDescription = accessibleButtonText
                                    },
                                    onClick = {
                                        handleEdit(info.clazz)
                                    }
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = info.icon,
                                        contentDescription = null
                                    )
                                    Text(stringResource(info.title))
                                }
                            }
                        }
                        if (criteriaSet.value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.filter_dialog_empty),
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .wrapContentHeight(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Row(
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 16.dp)
                                    .selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                val options = listOf(R.string.matchAll, R.string.matchAny)
                                options.forEachIndexed { index, labelRes ->
                                    Row(
                                        Modifier
                                            .weight(1f)
                                            .selectable(
                                                selected = index == selectedComplex,
                                                onClick = { selectedComplex = index },
                                                role = Role.RadioButton
                                            ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            onClick = null,
                                            selected = index == selectedComplex
                                        )
                                        Text(
                                            text = stringResource(labelRes),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                Modifier
                                    .verticalScroll(rememberScrollState())
                                    .semantics {
                                        collectionInfo = CollectionInfo(criteriaSet.value.size, 1)
                                    }
                            ) {

                                criteriaSet.value.forEachIndexed { index, criterion ->
                                    val negate =
                                        { criteriaSet.value = criteriaSet.value.negate(index) }
                                    val delete = { criteriaSet.value -= criterion }
                                    val edit = {
                                        handleEdit(criterion)
                                    }
                                    val title = stringResource(criterion.displayTitle)
                                    val symbol = criterion.displaySymbol.first
                                    val prettyPrint = ((criterion as? NotCriterion)?.criterion
                                        ?: criterion).prettyPrint(LocalContext.current)
                                    val contentDescription =
                                        criterion.contentDescription(LocalContext.current)
                                    val labelNegate = stringResource(R.string.negation)
                                    val labelDelete = stringResource(R.string.menu_delete)
                                    val labelEdit = stringResource(R.string.menu_edit)
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .clearAndSetSemantics {
                                                this.contentDescription = contentDescription
                                                collectionItemInfo =
                                                    CollectionItemInfo(index, 1, 1, 1)
                                                customActions = listOf(
                                                    CustomAccessibilityAction(
                                                        label = labelNegate,
                                                        action = {
                                                            negate()
                                                            true
                                                        }
                                                    ),
                                                    CustomAccessibilityAction(
                                                        label = labelDelete,
                                                        action = {
                                                            delete()
                                                            true
                                                        }
                                                    ),
                                                    CustomAccessibilityAction(
                                                        label = labelEdit,
                                                        action = {
                                                            edit()
                                                            true
                                                        }
                                                    )
                                                )
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = criterion.displayIcon,
                                            contentDescription = title
                                        )
                                        ActionButton(labelNegate, onClick = negate) {
                                            CharIcon(symbol)
                                        }
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = prettyPrint
                                        )
                                        ActionButton(
                                            hintText = labelDelete,
                                            icon = Icons.Filled.Delete,
                                            onClick = delete
                                        )
                                        ActionButton(
                                            hintText = labelEdit,
                                            icon = Icons.Filled.Edit,
                                            onClick = edit
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        filters.forEach { info ->
                            val definedCriterion = criteriaSet.value.find { it::class == info.clazz }
                            val hasCriterion = definedCriterion != null
                            Row(
                                Modifier
                                    .toggleable(
                                        value = hasCriterion,
                                        role = Role.Checkbox,
                                        onValueChange = {
                                            if (hasCriterion) {
                                                onConfirmRequest(
                                                    TYPE_QUICK,
                                                    (criteriaSet.value - definedCriterion).wrap()
                                                )
                                            } else {
                                                handleEdit(info.clazz)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.padding(end = 4.dp),
                                    imageVector = info.icon,
                                    contentDescription = null
                                )
                                Text(
                                    definedCriterion?.prettyPrint(LocalContext.current)
                                        ?: stringResource(info.title), Modifier.weight(1f)
                                )
                                Checkbox(checked = hasCriterion, onCheckedChange = null)
                            }
                        }
                    }
                }
            }

            if (confirmDiscard) {
                AlertDialog(
                    onDismissRequest = { confirmDiscard = false },
                    confirmButton = {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(R.string.response_yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDiscard = false }) {
                            Text(stringResource(R.string.response_no))
                        }
                    },
                    text = {
                        Text(stringResource(R.string.dialog_confirm_discard_changes))
                    }
                )

            }

            if (confirmClear) {
                AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmClear = false
                            criteriaSet.value = emptySet()
                        }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmClear = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                    text = {
                        Text(stringResource(R.string.clear_all_filters))
                    }
                )

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(
    hintText: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ActionButton(hintText, modifier, enabled, onClick) {
        Icon(
            imageVector = icon,
            contentDescription = hintText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(
    hintText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content:  @Composable () -> Unit
) {
    //workaround for https://issuetracker.google.com/issues/283821298
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(hintText) } },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = onClick, enabled = enabled, content = content)
        }
    }
}

private fun Set<Criterion>.negate(atIndex: Int) = mapIndexed { index, criterion ->
    if (index == atIndex) {
        if (criterion is NotCriterion) criterion.criterion else NotCriterion(criterion)
    } else criterion
}.toSet()

private fun Set<Criterion>.wrap(selectedComplex: Int = COMPLEX_AND) = when (size) {
    0 -> null
    1 -> first()
    else -> if (selectedComplex == COMPLEX_AND) AndCriterion(this) else OrCriterion(this)
}

/**
 * we can switch to quick search, if
 * - there are no criteria
 * - there are no negated criteria
 * - there is only 1 criterion
 * - criteria are joined with AND and there are no 2 criteria of the same type
 */
private fun Set<Criterion>.isSimple(selectedComplex: Int) =
    isEmpty() || (
            none { it is NotCriterion } && (
                    (size == 1) || (
                            (selectedComplex == COMPLEX_AND) && groupBy { it::class }.all { it.value.size == 1 }
                            )
                    )
            )

@Preview
@Composable
fun FilterDialogEmpty() {
    FilterDialog(
        account = FullAccount(
            id = 1,
            label = "Test account",
            currencyUnit = CurrencyUnit.DebugInstance,
            type = AccountType.CASH
        ),
        sumInfo = SumInfo.EMPTY,
    )
}

@Preview
@Composable
fun FilterDialogPreview() {
    FilterDialog(
        account = FullAccount(
            id = 1,
            label = "Test account",
            currencyUnit = CurrencyUnit.DebugInstance,
            type = AccountType.CASH,
        ),
        sumInfo = SumInfo.EMPTY,
        criterion = AndCriterion(
            setOf(
                NotCriterion(CommentCriterion("search")),
                CommentCriterion("search")
            )
        )
    )
}