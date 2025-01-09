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
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.window.DialogProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PickCategoryContract
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.ComplexCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.NotCriterion
import org.totschnig.myexpenses.provider.filter.SimpleCriterion


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    accountId: Long,
    criterion: ComplexCriterion = AndCriterion(emptyList()),
    onDismissRequest: () -> Unit
) {
    val edit: MutableState<ComplexCriterion> = remember { mutableStateOf(criterion) }
    val getCategory = rememberLauncherForActivityResult(PickCategoryContract()) { criterion ->
        criterion?.let { edit.value = edit.value.plus(criterion) }
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
                        androidx.compose.material3.Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(android.R.string.cancel)
                        )
                    }
                    Text(
                        text = stringResource(R.string.menu_search),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { /* doSomething() */ }) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Done,
                            contentDescription = stringResource(android.R.string.ok)
                        )
                    }
                }
                val filters = listOf(
                    R.string.category,
                    R.string.amount,
                    R.string.notes,
                    R.string.status,
                    R.string.payer_or_payee,
                    R.string.method,
                    R.string.date,
                    R.string.transfer,
                    R.string.tags,
                    R.string.accounts
                )
                FlowRow {
                    filters.forEach {
                        TextButton(
                            onClick = {
                                getCategory.launch(accountId to null)
                            }
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add filter"
                            )
                            Text(stringResource(it))
                        }
                    }
                }
                Column {
                    var selectedIndex by remember { mutableIntStateOf(0) }
                    val options =
                        listOf("Alle Bedingungen erfüllen", "Mindestens eine Bedingung erfüllen")
                    options.forEachIndexed { index, label ->
                        Row {
                            RadioButton(
                                onClick = { selectedIndex = index },
                                selected = index == selectedIndex
                            )
                            Text(label)
                        }
                    }
                }

                edit.value.criteria.forEachIndexed { index, criterion ->
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
                            text = ((criterion as? NotCriterion)?.criterion ?: criterion).prettyPrint(LocalContext.current))
                        IconButton(
                            onClick = { edit.value = edit.value.minus(criterion) }
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.menu_delete)
                            )
                        }
                    }
                }
            }
        }
    }
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
        accountId = 0L,
        criterion = AndCriterion(
            listOf(
                NotCriterion(CommentCriterion("search"))
            )
        ),
        onDismissRequest = {}
    )
}