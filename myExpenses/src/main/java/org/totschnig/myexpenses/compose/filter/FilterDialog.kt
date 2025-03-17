package org.totschnig.myexpenses.compose.filter

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PickCategoryContract
import org.totschnig.myexpenses.activity.PickPayeeContract
import org.totschnig.myexpenses.activity.PickTagContract
import org.totschnig.myexpenses.compose.CharIcon
import org.totschnig.myexpenses.compose.TEST_TAG_DIALOG
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.dialog.AmountFilterDialog
import org.totschnig.myexpenses.dialog.DateFilterDialog
import org.totschnig.myexpenses.dialog.KEY_RESULT_FILTER
import org.totschnig.myexpenses.dialog.RC_CONFIRM_FILTER
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment
import org.totschnig.myexpenses.model.AccountType
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
import org.totschnig.myexpenses.viewmodel.data.FullAccount

const val COMPLEX_AND = 0
const val COMPLEX_OR = 1

const val TYPE_QUICK = 0
const val TYPE_COMPLEX = 1

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    account: FullAccount?,
    sumInfo: SumInfo,
    preferredSearchType: Int = TYPE_COMPLEX,
    setPreferredSearchType: (Int) -> Unit = {},
    criterion: Criterion? = null,
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: (Criterion?) -> Unit = {},
) {

    val initialSet = criterion.asSet
    val initialSelectedComplex = if (criterion is OrCriterion) COMPLEX_OR else COMPLEX_AND
    val isComplexSearch = preferredSearchType == TYPE_COMPLEX

    var selectedComplex by remember {
        mutableIntStateOf(initialSelectedComplex)
    }

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

    val currentEdit: MutableState<Criterion?> = rememberSaveable {
        mutableStateOf(null)
    }

    val onResult: (Criterion?) -> Unit = remember(isComplexSearch) {
        { newValue ->
            if (newValue != null) {
                if (isComplexSearch) {
                    currentEdit.value?.also { current ->
                        criteriaSet.value = criteriaSet.value.map {
                            if (it == current)
                                if (it is NotCriterion) NotCriterion(newValue) else newValue
                            else it
                        }.toSet()
                    } ?: run { criteriaSet.value += newValue }
                } else {
                    onConfirmRequest(if (initialSet.isEmpty()) newValue else AndCriterion(initialSet + newValue))
                }
            }
            currentEdit.value = null
        }

    }

    val getCategory = rememberLauncherForActivityResult(PickCategoryContract(), onResult)
    val getPayee = rememberLauncherForActivityResult(PickPayeeContract(), onResult)
    val getTags = rememberLauncherForActivityResult(PickTagContract(), onResult)
    var showCommentFilterPrompt by rememberSaveable { mutableStateOf<CommentCriterion?>(null) }
    val activity = LocalActivity.current as? FragmentActivity

    fun handleAmountCriterion(criterion: AmountCriterion?) {
        AmountFilterDialog.newInstance(
            account!!.currencyUnit, criterion
        ).show(activity!!.supportFragmentManager, "AMOUNT_FILTER")
    }

    fun handleCommentCriterion(criterion: CommentCriterion?) {
        showCommentFilterPrompt = criterion ?: CommentCriterion("")
    }

    fun handleCrStatusCriterion(criterion: CrStatusCriterion?) {
        SelectCrStatusDialogFragment.newInstance(criterion)
            .show(activity!!.supportFragmentManager, "STATUS_FILTER")
    }

    fun handleDateCriterion(criterion: DateCriterion?) {
        DateFilterDialog.newInstance(criterion)
            .show(activity!!.supportFragmentManager, "DATE_FILTER")
    }

    fun handleAccountCriterion(criterion: AccountCriterion?) {
        SelectMultipleAccountDialogFragment.newInstance(
            if (account!!.isHomeAggregate) null else account.currency,
            criterion
        )
            .show(activity!!.supportFragmentManager, "ACCOUNT_FILTER")
    }

    fun handleCategoryCriterion(criterion: CategoryCriterion?) {
        getCategory.launch(account!!.id to criterion)
    }

    fun handleMethodCriterion(criterion: MethodCriterion?) {
        SelectMethodDialogFragment.newInstance(
            account!!.id, criterion
        ).show(activity!!.supportFragmentManager, "METHOD_FILTER")
    }

    fun handlePayeeCriterion(criterion: PayeeCriterion?) {
        getPayee.launch(account!!.id to criterion)
    }

    fun handleTagCriterion(criterion: TagCriterion?) {
        getTags.launch(account!!.id to criterion)
    }

    fun handleTransferCriterion(criterion: TransferCriterion?) {
        SelectTransferAccountDialogFragment.newInstance(account!!.id, criterion)
            .show(activity!!.supportFragmentManager, "TRANSFER_FILTER")
    }

    fun handleEdit(criterion: Criterion) {
        when (criterion) {
            is NotCriterion -> handleEdit(criterion.criterion)
            is AmountCriterion -> handleAmountCriterion(criterion)
            is CrStatusCriterion -> handleCrStatusCriterion(criterion)
            is DateCriterion -> handleDateCriterion(criterion)
            is AccountCriterion -> handleAccountCriterion(criterion)
            is CategoryCriterion -> handleCategoryCriterion(criterion)
            is MethodCriterion -> handleMethodCriterion(criterion)
            is PayeeCriterion -> handlePayeeCriterion(criterion)
            is TagCriterion -> handleTagCriterion(criterion)
            is TransferCriterion -> handleTransferCriterion(criterion)
            is CommentCriterion -> handleCommentCriterion(criterion)
            else -> throw IllegalStateException("Nested complex not supported")
        }
    }
    activity?.let {
        val supportFragmentManager = it.supportFragmentManager
        DisposableEffect(onResult) {
            supportFragmentManager.setFragmentResultListener(
                RC_CONFIRM_FILTER, it
            ) { _, result ->
                onResult(
                    BundleCompat.getParcelable(
                        result,
                        KEY_RESULT_FILTER,
                        SimpleCriterion::class.java
                    )
                )
            }
            onDispose {
                supportFragmentManager.clearFragmentResultListener(RC_CONFIRM_FILTER)
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
        properties = DialogProperties(usePlatformDefaultWidth = isLarge),
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
            Column(
                modifier = Modifier
                    .conditional(isLarge && criteriaSet.value.isEmpty()) {
                        height(400.dp)
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val options = listOf("Quick search", "Complex search")
                    ExposedDropdownMenuBox(
                        modifier = Modifier.align(Alignment.Center),
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }) {
                        BasicTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = options[preferredSearchType],
                            onValueChange = {},
                            textStyle = MaterialTheme.typography.titleMedium
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
                                    onClick = {
                                        setPreferredSearchType(index)
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
                        onclick = onDismiss
                    )
                    if (isComplexSearch) {
                        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                            ActionButton(
                                hintText = stringResource(R.string.clear_all_filters),
                                icon = Icons.Filled.ClearAll,
                                enabled = criteriaSet.value.isNotEmpty()
                            ) {
                                criteriaSet.value = emptySet()
                            }
                            ActionButton(
                                hintText = stringResource(R.string.apply),
                                icon = Icons.Filled.Done,
                                enabled = isDirty
                            ) {
                                onConfirmRequest(criteriaSet.value.wrap(selectedComplex))
                            }
                        }
                    }
                }

                val filters: List<Pair<DisplayInfo, () -> Unit>> = listOfNotNull(
                    if (sumInfo.mappedCategories) {
                        CategoryCriterion to { handleCategoryCriterion(null) }
                    } else null,
                    AmountCriterion to { handleAmountCriterion(null) },
                    CommentCriterion to { handleCommentCriterion(null) },
                    if (account?.isAggregate == true || account?.type != AccountType.CASH) {
                        CrStatusCriterion to { handleCrStatusCriterion(null) }
                    } else null,
                    if (sumInfo.mappedPayees) {
                        PayeeCriterion to { handlePayeeCriterion(null) }
                    } else null,
                    if (sumInfo.mappedMethods) {
                        MethodCriterion to { handleMethodCriterion(null) }
                    } else null,
                    DateCriterion to { handleDateCriterion(null) },
                    if (sumInfo.hasTransfers) {
                        TransferCriterion to { handleTransferCriterion(null) }
                    } else null,
                    if (sumInfo.hasTags) {
                        TagCriterion to { handleTagCriterion(null) }
                    } else null,
                    if (account?.isAggregate == true) {
                        AccountCriterion to { handleAccountCriterion(null) }
                    } else null
                )
                if (isComplexSearch) {
                    FlowRow {
                        filters.forEach { (info, onClick) ->
                            val accessibleButtonText =
                                stringResource(R.string.add_filter) + ": " +
                                        stringResource(info.extendedTitle)
                            TextButton(
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = accessibleButtonText
                                },
                                onClick = onClick
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
                                val negate = { criteriaSet.value = criteriaSet.value.negate(index) }
                                val delete = { criteriaSet.value -= criterion }
                                val edit = {
                                    currentEdit.value = criterion
                                    handleEdit(criterion)
                                }
                                val title = stringResource(criterion.displayTitle)
                                val symbol = criterion.displaySymbol.first
                                val prettyPrint = ((criterion as? NotCriterion)?.criterion
                                    ?: criterion).prettyPrint(LocalContext.current)
                                val contentDescription =
                                    criterion.contentDescription(LocalContext.current)
                                val labelDelete = stringResource(R.string.menu_delete)
                                val labelEdit = stringResource(R.string.menu_edit)
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .clearAndSetSemantics {
                                            this.contentDescription = contentDescription
                                            collectionItemInfo = CollectionItemInfo(index, 1, 1, 1)
                                            customActions = listOf(
                                                CustomAccessibilityAction(
                                                    label = "Negate",
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
                                                        edit
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
                                    IconButton(
                                        modifier = Modifier.semantics {
                                            invisibleToUser()
                                        },
                                        onClick = negate
                                    ) {
                                        CharIcon(symbol)
                                    }
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = prettyPrint
                                    )
                                    IconButton(
                                        onClick = delete
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = labelDelete
                                        )
                                    }
                                    IconButton(
                                        onClick = edit
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = labelEdit
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    filters.forEach { (info, onClick) ->
                        val definedCriterion = initialSet.find { it::class == info.clazz }
                        val hasCriterion = definedCriterion != null
                        Row(
                            Modifier
                                .toggleable(
                                    value = hasCriterion,
                                    role = Role.Checkbox,
                                    onValueChange = {
                                        if (hasCriterion) {
                                            onConfirmRequest((initialSet - definedCriterion).wrap())
                                        } else {
                                            onClick()
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                            , verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(definedCriterion?.prettyPrint(LocalContext.current)
                                ?: stringResource(info.title), Modifier.weight(1f))
                            Checkbox(checked = hasCriterion, onCheckedChange = null)
                        }
                    }
                }
            }

            showCommentFilterPrompt?.let {
                var search by rememberSaveable { mutableStateOf(it.searchString) }

                AlertDialog(
                    onDismissRequest = {
                        showCommentFilterPrompt = null
                        onResult(null)
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (search.isNotEmpty()) {
                                onResult(CommentCriterion(search))
                            }
                            showCommentFilterPrompt = null
                        }) {
                            Text(stringResource(id = android.R.string.ok))
                        }
                    },
                    text = {
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current
                        OutlinedTextField(
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        keyboardController?.show()
                                    }
                                },
                            value = search,
                            onValueChange = {
                                search = it
                            },
                            label = { Text(text = stringResource(R.string.search_comment)) },
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                )
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
    onclick: () -> Unit,
) {
    //workaround for https://issuetracker.google.com/issues/283821298
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(hintText) } },
            state = rememberTooltipState(),
            modifier = modifier
        ) {
            IconButton(onClick = onclick, enabled = enabled) {
                Icon(
                    imageVector = icon,
                    contentDescription = hintText
                )
            }
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

@Preview
@Composable
fun FilterDialogEmpty() {
    FilterDialog(
        account = null,
        sumInfo = SumInfo.EMPTY,
    )
}

@Preview
@Composable
fun FilterDialogPreview() {
    FilterDialog(
        account = null,
        sumInfo = SumInfo.EMPTY,
        criterion = AndCriterion(
            setOf(
                NotCriterion(CommentCriterion("search")),
                CommentCriterion("search")
            )
        )
    )
}