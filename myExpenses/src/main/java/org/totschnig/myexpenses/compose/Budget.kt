package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.Category

@Composable
fun Budget(
    modifier: Modifier = Modifier,
    category: Category,
    expansionMode: ExpansionMode,
    currency: CurrencyUnit,
    startPadding: Dp = 0.dp,
    parent: Category? = null,
    onBudgetEdit: (category: Category, parent: Category?) -> Unit,
    onShowTransactions: (category: Category) -> Unit,
    hasRolloverNext: Boolean
) {
    Column(
        modifier = modifier.then(
            if (category.level == 0) {
                (if (narrowScreen) {
                    Modifier.horizontalScroll(
                        rememberScrollState()
                    )
                } else Modifier)
                    .padding(horizontal = dimensionResource(id = R.dimen.activity_horizontal_margin))
            } else Modifier
        )
    ) {
        val doEdit = { onBudgetEdit(category, parent) }
        val doShow = { onShowTransactions(category) }
        if (category.level > 0) {
            BudgetCategoryRenderer(
                category = category,
                currency = currency,
                expansionMode = expansionMode,
                startPadding = startPadding,
                onBudgetEdit = doEdit,
                onShowTransactions = doShow,
                hasRolloverNext = hasRolloverNext
            )
            AnimatedVisibility(visible = expansionMode.isExpanded(category.id)) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    category.children.forEach { model ->
                        Budget(
                            category = model,
                            expansionMode = expansionMode,
                            currency = currency,
                            startPadding = startPadding + 12.dp,
                            parent = category,
                            onBudgetEdit = onBudgetEdit,
                            onShowTransactions = onShowTransactions,
                            hasRolloverNext = hasRolloverNext
                        )
                    }
                }
            }
        } else {
            Header(withRollOverColumn = hasRolloverNext)
            Divider(modifier = if (narrowScreen) Modifier.width(tableWidth) else Modifier)
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Summary(
                        category,
                        currency,
                        doEdit,
                        doShow,
                        hasRolloverNext
                    )
                    Divider(modifier = if (narrowScreen) Modifier.width(tableWidth) else Modifier)
                }
                category.children.forEach { model ->
                    item {
                        Budget(
                            category = model,
                            parent = category,
                            expansionMode = expansionMode,
                            currency = currency,
                            onBudgetEdit = onBudgetEdit,
                            onShowTransactions = onShowTransactions,
                            hasRolloverNext = hasRolloverNext
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(
    category: Category,
    currency: CurrencyUnit,
    onBudgetEdit: () -> Unit,
    onShowTransactions: () -> Unit,
    hasRolloverNext: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .labelColumn(this),
            fontWeight = FontWeight.Bold,
            text = stringResource(id = R.string.menu_aggregates)
        )
        VerticalDivider()
        BudgetNumbers(category, currency, onBudgetEdit, onShowTransactions, hasRolloverNext)
    }
}

@Composable
private fun VerticalDivider() {
    Divider(
        modifier = Modifier
            .height(48.dp)
            .width(1.dp)
    )
}

val breakPoint = 600.dp
const val labelFraction = 0.35f
const val numberFraction = 0.2f
val verticalDividerWidth = 1.dp
val tableWidth = breakPoint * (labelFraction + 3 * numberFraction) + verticalDividerWidth * 3

val narrowScreen: Boolean
    @Composable get() = LocalConfiguration.current.screenWidthDp < breakPoint.value

private fun Modifier.labelColumn(scope: RowScope): Modifier =
    composed { this.then(if (narrowScreen) width(breakPoint * 0.35f) else with(scope) { weight(2f) }) }.padding(
        end = 8.dp
    )

private fun Modifier.numberColumn(scope: RowScope): Modifier =
    composed { this.then(if (narrowScreen) width(breakPoint * 0.2f) else with(scope) { weight(1f) }) }
        .padding(horizontal = 8.dp)

@Composable
private fun Header(withRollOverColumn: Boolean) {
    @Composable
    fun RowScope.HeaderCell(stringRes: Int) {
        Text(
            modifier = Modifier
                .numberColumn(this),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            text = stringResource(id = stringRes)
        )
    }

    Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .labelColumn(this)
        )
        VerticalDivider()
        HeaderCell(R.string.budget_table_header_allocated)
        VerticalDivider()
        HeaderCell(R.string.budget_table_header_spent)
        VerticalDivider()
        HeaderCell(R.string.budget_table_header_remainder)
        if (withRollOverColumn) {
            VerticalDivider()
            HeaderCell(R.string.budget_table_header_rollover)
        }
    }
}

@Composable
private fun BudgetCategoryRenderer(
    category: Category,
    currency: CurrencyUnit,
    expansionMode: ExpansionMode,
    startPadding: Dp,
    onBudgetEdit: () -> Unit,
    onShowTransactions: () -> Unit,
    hasRolloverNext: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .labelColumn(this)
                .padding(start = startPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isExpanded = expansionMode.isExpanded(category.id)
            Text(modifier = Modifier.weight(1f, false), text = category.label)
            if (category.children.isNotEmpty()) {
                IconButton(onClick = { expansionMode.toggle(category = category) }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            id = if (isExpanded)
                                R.string.content_description_collapse else
                                R.string.content_description_expand
                        )
                    )
                }
            }
        }
        VerticalDivider()
        BudgetNumbers(category = category, currency = currency, onBudgetEdit, onShowTransactions, hasRolloverNext)
    }
}

@Composable
private fun RowScope.BudgetNumbers(
    category: Category,
    currency: CurrencyUnit,
    onBudgetEdit: () -> Unit,
    onShowTransactions: () -> Unit,
    hasRolloverNext: Boolean
) {
    //Allocation
    val allocation =
        if (category.children.isEmpty()) category.budget.budget else category.children.sumOf { it.budget.budget }
    Column(modifier = Modifier.numberColumn(this)) {
        Text(
            modifier = Modifier
                .clickable(onClick = onBudgetEdit)
                .fillMaxWidth(),
            text = LocalAmountFormatter.current(category.budget.budget, currency),
            textAlign = TextAlign.End,
            textDecoration = TextDecoration.Underline
        )
        if (category.budget.rollOverPrevious != 0L) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = LocalAmountFormatter.current(category.budget.rollOverPrevious, currency),
                textAlign = TextAlign.End,
                color = LocalColors.current.budgetRollOver
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = " = " + LocalAmountFormatter.current(
                    category.budget.budget + category.budget.rollOverPrevious,
                    currency
                ),
                textAlign = TextAlign.End,
                color = LocalColors.current.budgetRollOver
            )
        }
        if (allocation != category.budget.totalAllocated && allocation != 0L) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "(${LocalAmountFormatter.current(allocation, currency)})",
                textAlign = TextAlign.End
            )
        }
    }

    VerticalDivider()

    //Spent
    val aggregateSum = category.aggregateSum
    Text(
        modifier = Modifier
            .numberColumn(this)
            .clickable(onClick = onShowTransactions),
        text = LocalAmountFormatter.current(aggregateSum, currency),
        textAlign = TextAlign.End,
        textDecoration = TextDecoration.Underline
    )

    VerticalDivider()

    //Remainder
    val remainder = category.budget.totalAllocated + aggregateSum
    ColoredAmountText(
        modifier = Modifier.numberColumn(this),
        amount = remainder,
        currency = currency,
        textAlign = TextAlign.End,
        withBorder = true
    )

    //Rollover
    if (hasRolloverNext) {
        VerticalDivider()
        Column(
            modifier = Modifier.numberColumn(this),
            horizontalAlignment = Alignment.End
        ) {
            if (category.budget.rollOverNext != 0L) {
                Text(
                    text = LocalAmountFormatter.current(category.budget.rollOverNext, currency),
                    textAlign = TextAlign.End,
                    color = LocalColors.current.budgetRollOver
                )
            }
            val rollOverFromChildren = category.aggregateRollOverNext
            if (rollOverFromChildren != 0L) {
                Text(
                    text = "(" + LocalAmountFormatter.current(
                        rollOverFromChildren,
                        currency
                    ) + ")",
                    textAlign = TextAlign.End,
                    color = LocalColors.current.budgetRollOver
                )
            }
        }
    }
}