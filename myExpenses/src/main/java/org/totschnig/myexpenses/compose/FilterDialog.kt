package org.totschnig.myexpenses.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PickCategoryContract
import org.totschnig.myexpenses.activity.PickPayeeContract
import org.totschnig.myexpenses.activity.PickTagContract
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
import org.totschnig.myexpenses.provider.filter.ComplexCriterion
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
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.lang.IllegalStateException

const val COMPLEX_AND = 0
const val COMPLEX_OR = 1

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    account: FullAccount?,
    sumInfo: SumInfo,
    criterion: Criterion? = null,
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: (Criterion?) -> Unit = {},
) {
    val (selectedComplex, oComplexSelected) = remember {
        mutableIntStateOf(
            if (criterion is OrCriterion) COMPLEX_OR else COMPLEX_AND
        )
    }
    val criteriaSet: MutableState<Set<Criterion>> =
        remember {
            mutableStateOf(
                (criterion as? ComplexCriterion)?.criteria
                    ?: criterion?.let { setOf(it) }
                    ?: emptySet()
            )
        }
    val currentEdit: MutableState<Criterion?> = remember {
        mutableStateOf(null)
    }
    val onResult: (Criterion?) -> Unit = { newValue ->
        if (newValue != null) {
            currentEdit.value?.also { current ->
                criteriaSet.value = criteriaSet.value.map {
                    if (it == current)
                        if (it is NotCriterion) NotCriterion(newValue) else newValue
                    else it
                }.toSet()
                currentEdit.value = null
            } ?: run { criteriaSet.value += newValue }
        }
    }

    val getCategory = rememberLauncherForActivityResult(PickCategoryContract(), onResult)
    val getPayee = rememberLauncherForActivityResult(PickPayeeContract(), onResult)
    val getTags = rememberLauncherForActivityResult(PickTagContract(), onResult)
    var showCommentFilterPrompt by remember { mutableStateOf<CommentCriterion?>(null) }
    val activity = LocalContext.current as? FragmentActivity

    fun handleAmountCriterion(criterion: AmountCriterion?) {
        AmountFilterDialog.newInstance(
            account!!.currencyUnit, criterion
        ).show(activity!!.supportFragmentManager, "AMOUNT_FILTER")
    }

    fun handleCommentCriterion(criterion: CommentCriterion?) {
        showCommentFilterPrompt = criterion ?: CommentCriterion("")
    }

    fun handleEdit(criterion: Criterion) {
        when (criterion) {
            is NotCriterion -> handleEdit(criterion.criterion)
            is AmountCriterion -> handleAmountCriterion(criterion)
            is CrStatusCriterion -> TODO()
            is DateCriterion -> TODO()
            is AccountCriterion -> TODO()
            is CategoryCriterion -> TODO()
            is MethodCriterion -> TODO()
            is PayeeCriterion -> TODO()
            is TagCriterion -> TODO()
            is TransferCriterion -> TODO()
            is CommentCriterion -> handleCommentCriterion(criterion)
            else -> throw IllegalStateException("Nested complex not supported")
        }
    }
    activity?.let {
        val supportFragmentManager = it.supportFragmentManager
        DisposableEffect(Unit) {
            supportFragmentManager.setFragmentResultListener(
                RC_CONFIRM_FILTER, it
            ) { _, result ->
                BundleCompat.getParcelable(result, KEY_RESULT_FILTER, SimpleCriterion::class.java)
                    ?.let { onResult(it) }
            }
            onDispose {
                supportFragmentManager.clearFragmentResultListener(RC_CONFIRM_FILTER)
            }
        }
    }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(android.R.string.cancel)
                        )
                    }
                    Text(
                        text = stringResource(R.string.menu_search),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(
                        onClick = {
                            onConfirmRequest(
                                when (criteriaSet.value.size) {
                                    0 -> null
                                    1 -> criteriaSet.value.first()
                                    else -> if (selectedComplex == COMPLEX_AND)
                                        AndCriterion(criteriaSet.value) else OrCriterion(criteriaSet.value)
                                }
                            )
                        }) {
                        Icon(
                            Icons.Filled.Done,
                            contentDescription = stringResource(android.R.string.ok)
                        )
                    }
                }

                val filters: List<Pair<DisplayInfo, () -> Unit>> = listOfNotNull(
                    if (sumInfo.mappedCategories) {
                        CategoryCriterion to { getCategory.launch(account!!.id to null) }
                    } else null,
                    AmountCriterion to { handleAmountCriterion(null) },
                    CommentCriterion to { handleCommentCriterion(null) },
                    if (account?.isAggregate == true || account?.type != AccountType.CASH) {
                        CrStatusCriterion to {
                            SelectCrStatusDialogFragment.newInstance(null)
                                .show(activity!!.supportFragmentManager, "STATUS_FILTER")
                        }
                    } else null,
                    if (sumInfo.mappedPayees) {
                        PayeeCriterion to { getPayee.launch(account!!.id to null) }
                    } else null,
                    if (sumInfo.mappedMethods) {
                        MethodCriterion to {
                            SelectMethodDialogFragment.newInstance(
                                account!!.id, null
                            ).show(activity!!.supportFragmentManager, "METHOD_FILTER")
                        }
                    } else null,
                    DateCriterion to {
                        DateFilterDialog.newInstance(null)
                            .show(activity!!.supportFragmentManager, "DATE_FILTER")
                    },
                    if (sumInfo.hasTransfers) {
                        TransferCriterion to {
                            SelectTransferAccountDialogFragment.newInstance(account!!.id, null)
                                .show(activity!!.supportFragmentManager, "TRANSFER_FILTER")
                        }
                    } else null,
                    if (sumInfo.hasTags) {
                        TagCriterion to { getTags.launch(account!!.id to null) }
                    } else null,
                    if (account?.isAggregate == true) {
                        AccountCriterion to {
                            SelectMultipleAccountDialogFragment.newInstance(
                                if (account.isHomeAggregate) null else account.currency,
                                null
                            )
                                .show(activity!!.supportFragmentManager, "ACCOUNT_FILTER")
                        }
                    } else null
                )
                FlowRow {
                    filters.forEach { (info, onClick) ->
                        TextButton(onClick = onClick) {
                            Icon(
                                modifier = Modifier.padding(end = 4.dp),
                                imageVector = info.icon,
                                contentDescription = "Add filter"
                            )
                            Text(stringResource(info.title))
                        }
                    }
                }
                Row(
                    Modifier
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 16.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val options =
                        listOf("Alle Bedingungen erfüllen", "Mindestens eine Bedingung erfüllen")
                    options.forEachIndexed { index, label ->
                        Row(
                            Modifier
                                .weight(1f)
                                .selectable(
                                    selected = index == selectedComplex,
                                    onClick = { oComplexSelected(index) },
                                    role = Role.RadioButton
                                ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                onClick = null,
                                selected = index == selectedComplex
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                criteriaSet.value.forEachIndexed { index, criterion ->
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = criterion.displayIcon,
                            contentDescription = stringResource(criterion.displayTitle)
                        )
                        IconButton(
                            onClick = { criteriaSet.value = criteriaSet.value.negate(index) }
                        ) {
                            CharIcon(if (criterion is NotCriterion) '≠' else '=', size = 18.sp)
                        }
                        Text(
                            modifier = Modifier.weight(1f),
                            text = ((criterion as? NotCriterion)?.criterion
                                ?: criterion).prettyPrint(LocalContext.current)
                        )
                        IconButton(
                            onClick = { criteriaSet.value -= criterion }
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.menu_delete)
                            )
                        }
                        IconButton(
                            onClick = {
                                currentEdit.value = criterion
                                handleEdit(criterion)
                            }
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.menu_edit)
                            )
                        }
                    }
                }
            }
            showCommentFilterPrompt?.let {
                var search by rememberSaveable { mutableStateOf(it.searchString) }

                AlertDialog(
                    onDismissRequest = {
                        showCommentFilterPrompt = null
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
                        OutlinedTextField(
                            value = search,
                            onValueChange = {
                                search = it
                            },
                            label = { Text(text = stringResource(R.string.menu_search)) },
                        )
                    }
                )
            }
        }
    }
}

fun Set<Criterion>.negate(atIndex: Int) = mapIndexed { index, criterion ->
    if (index == atIndex) {
        if (criterion is NotCriterion) criterion.criterion else NotCriterion(criterion)
    } else criterion
}.toSet()

@Preview(device = "id:pixel")
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