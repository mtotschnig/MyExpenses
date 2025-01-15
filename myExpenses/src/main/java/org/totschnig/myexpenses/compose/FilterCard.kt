package org.totschnig.myexpenses.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.filter.AndCriterion
import org.totschnig.myexpenses.provider.filter.CommentCriterion
import org.totschnig.myexpenses.provider.filter.ComplexCriterion
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.NotCriterion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterCard(
    whereFilter: Criterion,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .testTag(TEST_TAG_FILTER_CARD)
            .background(color = colorResource(id = R.color.cardBackground))
            .padding(horizontal = dimensionResource(R.dimen.padding_main_screen), vertical = 4.dp)
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
                FilterItem(criterion, index)
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
            FilterItem(whereFilter)
        }
    }
}

@Composable
fun FilterItem(
    criterion: Criterion,
    index: Int? = null
) {
    val contentDescription = criterion.contentDescription(LocalContext.current)
    Row(
        Modifier
            .border(
                SuggestionChipDefaults.suggestionChipBorder(true),
                SuggestionChipDefaults.shape
            )
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 8.dp)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                index?.let {
                    collectionItemInfo = CollectionItemInfo(index, 1, -1, -1)
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = criterion.displayIcon,
            contentDescription = stringResource(criterion.displayTitle)
        )
        Text(criterion.prettyPrint(LocalContext.current))
    }
}

@Preview
@Composable
fun FilterCardPreview() {
    FilterCard(
        AndCriterion(
            setOf(
                NotCriterion(CommentCriterion("search")),
                CommentCriterion("search1"),
                CommentCriterion("search2"),
                CommentCriterion("search3"),
                CommentCriterion("search4"),
            )
        )
    )
}