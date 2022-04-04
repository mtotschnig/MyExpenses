package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.Category2

@Composable
fun Budget(
    modifier: Modifier = Modifier,
    category: Category2,
    expansionMode: ExpansionMode,
    currency: CurrencyUnit
) {
    Column(
        modifier = modifier
    ) {
        if (category.level > 0) {
            BudgetCategoryRenderer(
                category = category,
                currency = currency
            )
            AnimatedVisibility(visible = expansionMode.isExpanded(category.id)) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    category.children.forEach { model ->
                        Budget(
                            category = model,
                            expansionMode = expansionMode,
                            currency = currency
                        )
                    }
                }
            }
        } else {
            Header()
            Divider()
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                category.children.forEach { model ->
                    item {
                        Budget(
                            category = model,
                            expansionMode = expansionMode,
                            currency = currency
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Divider(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
    )
}

@Composable
private fun Header() {
    @Composable
    fun RowScope.HeaderCell(stringRes: Int) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
            text = stringResource(id = stringRes)
        )
    }
    Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .weight(2f)
                .padding(end = 8.dp)
        )
        HorizontalDivider()
        HeaderCell(R.string.budget_table_header_allocated)
        HorizontalDivider()
        HeaderCell(R.string.budget_table_header_spent)
        HorizontalDivider()
        HeaderCell(R.string.budget_table_header_remainder)
    }
}

@Composable
private fun BudgetCategoryRenderer(category: Category2, currency: CurrencyUnit) {
    Row(modifier = Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier
                .weight(2f)
                .padding(end = 8.dp), text = category.label
        )
        HorizontalDivider()
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable { }
            .weight(1f)
            .padding(horizontal = 8.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterEnd),
                text = LocalAmountFormatter.current(category.budget, currency),
                textAlign = TextAlign.End,
                textDecoration = TextDecoration.Underline
            )
        }
        HorizontalDivider()
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = LocalAmountFormatter.current(category.aggregateSum, currency),
            textAlign = TextAlign.End
        )
        HorizontalDivider()
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            ColoredAmountText(
                modifier = Modifier.align(Alignment.CenterEnd),
                amount = category.budget + category.aggregateSum,
                currency = currency,
                textAlign = TextAlign.End,
                withBorder = true
            )
        }
    }
}