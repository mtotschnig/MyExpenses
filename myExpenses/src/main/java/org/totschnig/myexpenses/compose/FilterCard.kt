package org.totschnig.myexpenses.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    Row(
        modifier = modifier
            .testTag(TEST_TAG_FILTER_CARD)
            .background(color = colorResource(id = R.color.cardBackground))
            .padding(horizontal = dimensionResource(R.dimen.padding_main_screen)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowRow(
            modifier = Modifier
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (whereFilter is ComplexCriterion) {
                whereFilter.criteria.forEachIndexed { index, criterion ->
                    FilterItem(criterion)
                    if (index < whereFilter.criteria.size - 1) {
                        CharIcon(whereFilter.symbol)
                    }
                }
            } else {
                FilterItem(whereFilter)
            }
        }
    }
}

@Composable
fun FilterItem(criterion: Criterion) {

    Row(
        Modifier
            .border(
                SuggestionChipDefaults.suggestionChipBorder(true),
                SuggestionChipDefaults.shape
            )
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 8.dp),
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
                CommentCriterion("search")
            )
        )
    )
}