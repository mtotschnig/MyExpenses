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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.totschnig.myexpenses.dialog.AmountFilterDialog
import org.totschnig.myexpenses.dialog.DateFilterDialog
import org.totschnig.myexpenses.dialog.KEY_RESULT_FILTER
import org.totschnig.myexpenses.dialog.RC_CONFIRM_FILTER
import org.totschnig.myexpenses.dialog.select.SelectCrStatusDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMethodDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectMultipleAccountDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectTransferAccountDialogFragment
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.ComplexCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.OrCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion
import org.totschnig.myexpenses.viewmodel.SumInfo
import org.totschnig.myexpenses.viewmodel.data.FullAccount

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
    var selectedComplex by remember {
        mutableIntStateOf(
            if (criterion is OrCriterion) COMPLEX_OR else COMPLEX_AND
        )
    }
    val edit: MutableState<List<Criterion>> =
        remember {
            mutableStateOf(
                (criterion as? ComplexCriterion)?.criteria
                    ?: criterion?.let { listOf(it) }
                    ?: emptyList()
            )
        }
    val onResult: (IdCriterion?) -> Unit = { crit -> crit?.let { edit.value += it } }
    val getCategory = rememberLauncherForActivityResult(PickCategoryContract(), onResult)
    val getPayee = rememberLauncherForActivityResult(PickPayeeContract(), onResult)
    val getTags = rememberLauncherForActivityResult(PickTagContract(), onResult)
    val activity = LocalContext.current as? FragmentActivity
    activity?.let {
        val supportFragmentManager = it.supportFragmentManager
        DisposableEffect(Unit) {
            supportFragmentManager.setFragmentResultListener(
                RC_CONFIRM_FILTER, it
            ) { _, result ->
                BundleCompat.getParcelable(result, KEY_RESULT_FILTER, SimpleCriterion::class.java)
                    ?.let { edit.value += it }
            }
            onDispose {
                supportFragmentManager.clearFragmentResultListener(RC_CONFIRM_FILTER)
            }
        }
    }

    var showCommentFilterPrompt by remember { mutableStateOf(false) }

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
                        androidx.compose.material3.Icon(
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
                                when (edit.value.size) {
                                    0 -> null
                                    1 -> edit.value.first()
                                    else -> if (selectedComplex == COMPLEX_AND)
                                        AndCriterion(edit.value) else OrCriterion(edit.value)
                                }
                            )
                        }) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Done,
                            contentDescription = stringResource(android.R.string.ok)
                        )
                    }
                }

                val filters: List<Pair<Int, () -> Unit>> = listOfNotNull(
                    if (sumInfo.mappedCategories) {
                        R.string.category to { getCategory.launch(account!!.id to null) }
                    } else null,
                    R.string.amount to {
                        AmountFilterDialog.newInstance(
                            account!!.currencyUnit, null
                        ).show(activity!!.supportFragmentManager, "AMOUNT_FILTER")
                    },
                    R.string.notes to {
                        showCommentFilterPrompt = true
                    },
                    if (account?.isAggregate == true || account?.type != AccountType.CASH) {
                        R.string.status to {
                            SelectCrStatusDialogFragment.newInstance(null)
                                .show(activity!!.supportFragmentManager, "STATUS_FILTER")
                        }
                    } else null,
                    if (sumInfo.mappedPayees) {
                        R.string.payer_or_payee to { getPayee.launch(account!!.id to null) }
                    } else null,
                    if (sumInfo.mappedMethods) {
                        R.string.method to {
                            SelectMethodDialogFragment.newInstance(
                                account!!.id, null
                            ).show(activity!!.supportFragmentManager, "METHOD_FILTER")
                        }
                    } else null,
                    R.string.date to {
                        DateFilterDialog.newInstance(null)
                            .show(activity!!.supportFragmentManager, "DATE_FILTER")
                    },
                    if (sumInfo.hasTransfers) {
                        R.string.transfer to {
                            SelectTransferAccountDialogFragment.newInstance(account!!.id, null)
                                .show(activity!!.supportFragmentManager, "TRANSFER_FILTER")
                        }
                    } else null,
                    if (sumInfo.hasTags) {
                        R.string.tags to { getTags.launch(account!!.id to null) }
                    } else null,
                    if (account?.isAggregate == true) {
                        R.string.accounts to {
                            SelectMultipleAccountDialogFragment.newInstance(
                                if (account.isHomeAggregate) null else account.currency,
                                null
                            )
                                .show(activity!!.supportFragmentManager, "ACCOUNT_FILTER")
                        }
                    } else null
                )
                FlowRow {
                    filters.forEach { (title, onClick) ->
                        TextButton(onClick = onClick) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add filter"
                            )
                            Text(stringResource(title))
                        }
                    }
                }
                Column {

                    val options =
                        listOf("Alle Bedingungen erfüllen", "Mindestens eine Bedingung erfüllen")
                    options.forEachIndexed { index, label ->
                        Row {
                            RadioButton(
                                onClick = { selectedComplex = index },
                                selected = index == selectedComplex
                            )
                            Text(label)
                        }
                    }
                }

                edit.value.forEachIndexed { index, criterion ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(criterion.displayTitle))
                        Button(
                            onClick = { edit.value = edit.value.negate(index) },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(if (criterion is NotCriterion) "!=" else "=")
                        }
                        Text(
                            modifier = Modifier.weight(1f),
                            text = ((criterion as? NotCriterion)?.criterion
                                ?: criterion).prettyPrint(LocalContext.current)
                        )
                        IconButton(
                            onClick = { edit.value -= criterion }
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.menu_delete)
                            )
                        }
                    }
                }
            }
            if (showCommentFilterPrompt) {
                var search by rememberSaveable { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = {
                        showCommentFilterPrompt = false
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (search.isNotEmpty()) {
                                edit.value += CommentCriterion(search)
                            }
                            showCommentFilterPrompt = false
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

fun List<Criterion>.negate(atIndex: Int) = mapIndexed { index, criterion ->
    if (index == atIndex) {
        if (criterion is NotCriterion) criterion.criterion else NotCriterion(criterion)
    } else criterion
}

val Criterion.displayTitle: Int
    get() = when (this) {
        is SimpleCriterion<*> -> title
        is NotCriterion -> criterion.displayTitle
        else -> throw NotImplementedError("Nested complex not supported")
    }

@Preview(device = "id:pixel_5")
@Composable
fun FilterDialogPreview() {
    FilterDialog(
        account = null,
        sumInfo = SumInfo.EMPTY,
        criterion = AndCriterion(
            listOf(
                NotCriterion(CommentCriterion("search"))
            )
        )
    )
}