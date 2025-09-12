package org.totschnig.myexpenses.compose.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.CharIcon
import org.totschnig.myexpenses.compose.TEST_TAG_FILTER_CARD
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.ComplexCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.NotCriterion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterCard(
    whereFilter: Criterion,
    editFilter: ((Criterion) -> Unit)? = null,
    clearAllFilter: (() -> Unit)? = null,
    clearFilter: ((Criterion) -> Unit)? = null,
) {
    if (clearAllFilter != null) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { newValue ->
                if (newValue != SwipeToDismissBoxValue.Settled) {
                    clearAllFilter()
                }
                false
            }
        )
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = dimensionResource(R.dimen.padding_main_screen)
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null
                    )
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null
                    )
                }
            }
        ) {
            FilterCardImpl(whereFilter, editFilter, clearFilter)
        }
    } else {
        FilterCardImpl(whereFilter, editFilter, clearFilter)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterCardImpl(
    whereFilter: Criterion,
    editFilter: ((Criterion) -> Unit)?,
    clearFilter: ((Criterion) -> Unit)? = null,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_FILTER_CARD)
            .background(color = colorResource(id = R.color.cardBackground))
            .padding(
                horizontal = dimensionResource(R.dimen.padding_main_screen),
                vertical = 4.dp
            )
            .optional(whereFilter as? ComplexCriterion) {
                semantics {
                    collectionInfo = CollectionInfo(it.criteria.size, 1)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        if (whereFilter is ComplexCriterion) {
            val separatorDescription = stringResource(whereFilter.description)
            whereFilter.criteria.forEachIndexed { index, criterion ->
                FilterItemImpl(criterion, index, editFilter, clearFilter)
                if (index < whereFilter.criteria.size - 1) {
                    CharIcon(
                        whereFilter.symbol,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .semantics { contentDescription = separatorDescription },
                        size = 12.sp
                    )
                }
            }
        } else {
            FilterItemImpl(whereFilter, editFilter = editFilter, clearFilter = clearFilter)
        }
    }
}

@Composable
fun FilterItemImpl(
    criterion: Criterion,
    index: Int? = null,
    editFilter: ((Criterion) -> Unit)?,
    clearFilter: ((Criterion) -> Unit)?,
) {
    val contentDescription = criterion.contentDescription(LocalContext.current)
    InputChip(
        modifier = Modifier.semantics {
            this.contentDescription = contentDescription
            index?.let {
                collectionItemInfo = CollectionItemInfo(index, 1, -1, -1)
            }
        },
        selected = true,
        onClick = { editFilter?.invoke(criterion) },
        label = { Text(criterion.prettyPrint(LocalContext.current)) },
        leadingIcon = {
            Icon(
                imageVector = criterion.displayIcon,
                contentDescription = stringResource(criterion.displayTitle)
            )
        },
        trailingIcon = if (clearFilter != null) {
            {
                Icon(
                    modifier = Modifier.clickable {
                        clearFilter(criterion)
                    },
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null
                )
            }
        } else null
    )
}

@Preview(widthDp = 350)
@Composable
fun FilterCardPreview() {
    FilterCard(
        AndCriterion(
            setOf(
                NotCriterion(CommentCriterion("search")),
                CommentCriterion("search1"),
            )
        )
    )
}