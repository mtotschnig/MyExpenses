package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.viewmodel.data.Category
import kotlin.math.absoluteValue
import kotlin.math.sign

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
    hasRolloverNext: Boolean,
    editRollOver: SnapshotStateMap<Long, Pair<Long, Boolean>>?,
    narrowScreen: Boolean,
    showChart: Boolean = false,
    currentSort: Sort? = null,
    onChangeSort: ((Sort) -> Unit)? = null
) {

    Column(
        modifier = modifier.conditional(category.level == 0) {
            conditional(narrowScreen) {
                horizontalScroll(rememberScrollState())
            }.padding(horizontal = dimensionResource(id = eltos.simpledialogfragment.R.dimen.activity_horizontal_margin))
        }
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
                hasRolloverNext = hasRolloverNext,
                editRollOver = editRollOver,
                narrowScreen = narrowScreen
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
                            hasRolloverNext = hasRolloverNext,
                            editRollOver = editRollOver,
                            narrowScreen = narrowScreen,
                            currentSort = currentSort,
                            onChangeSort = onChangeSort
                        )
                    }
                }
            }
        } else {
            Header(
                hasRolloverNext || editRollOver != null,
                narrowScreen,
                showChart,
                currentSort!!,
                onChangeSort!!
            )
            HorizontalDivider(modifier = if (narrowScreen) Modifier.width(tableWidth) else Modifier)
            LazyColumn(
                modifier = Modifier.testTag(TEST_TAG_LIST),
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Summary(
                        category,
                        currency,
                        doEdit,
                        doShow,
                        hasRolloverNext,
                        editRollOver,
                        narrowScreen
                    )
                    HorizontalDivider(modifier = if (narrowScreen) Modifier.width(tableWidth) else Modifier)
                }
                category.children.forEach { model ->
                    item {
                        Budget(
                            modifier = Modifier.testTag(TEST_TAG_ROW),
                            category = model,
                            expansionMode = expansionMode,
                            currency = currency,
                            parent = category,
                            onBudgetEdit = onBudgetEdit,
                            onShowTransactions = onShowTransactions,
                            hasRolloverNext = hasRolloverNext,
                            editRollOver = editRollOver,
                            narrowScreen = narrowScreen
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
    hasRolloverNext: Boolean,
    editRollOver: SnapshotStateMap<Long, Pair<Long, Boolean>>?,
    narrowScreen: Boolean
) {
    Row(
        modifier = Modifier.testTag(TEST_TAG_HEADER),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .labelColumn(this, narrowScreen),
            fontWeight = FontWeight.Bold,
            text = stringResource(id = R.string.menu_aggregates)
        )
        VerticalDivider()
        BudgetNumbers(
            category,
            currency,
            onBudgetEdit,
            onShowTransactions,
            hasRolloverNext,
            editRollOver,
            narrowScreen
        )
    }
}

@Composable
private fun VerticalDivider() {
    androidx.compose.material3.VerticalDivider(Modifier.height(48.dp))
}

val breakPoint = 600.dp
const val labelFraction = 0.35f
const val numberFraction = 0.2f
val verticalDividerWidth = 1.dp
val tableWidth = breakPoint * (labelFraction + 3 * numberFraction) + verticalDividerWidth * 3

private fun Modifier.labelColumn(scope: RowScope, narrowScreen: Boolean) =
    conditional(
        narrowScreen,
        ifTrue = { width(breakPoint * 0.35f) },
        ifFalse = { with(scope) { weight(2f) } }
    ).padding(end = 8.dp)

private fun Modifier.numberColumn(scope: RowScope, narrowScreen: Boolean): Modifier =
    conditional(
        narrowScreen,
        ifTrue = { width(breakPoint * 0.2f) },
        ifFalse = { with(scope) { weight(1f) } }
    ).padding(horizontal = 8.dp)

@Composable
private fun Header(
    withRollOverColumn: Boolean,
    narrowScreen: Boolean,
    showChart: Boolean,
    currentSort: Sort,
    onChangeSort: (Sort) -> Unit
) {
    @Composable
    fun RowScope.HeaderCell(
        stringRes: Int,
        sort: Sort? = null,
        color: Color? = null,
        isNumberColumn: Boolean = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .optional(sort.takeIf { it != currentSort }) {
                    clickable { onChangeSort(it) }
                }
                .conditional(
                    isNumberColumn,
                    ifTrue = { numberColumn(this@HeaderCell, narrowScreen) },
                    ifFalse = { labelColumn(this@HeaderCell, narrowScreen) }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            color?.let {
                Box(
                    modifier = Modifier
                        .size(12.sp)
                        .background(it)
                        .padding(end = 4.dp)
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                text = stringResource(id = stringRes),
            )
            if (currentSort == sort) {
                Icon(
                    imageVector = if (sort.isDescending) Icons.Default.ArrowDownward
                    else Icons.Default.ArrowUpward,
                    contentDescription = null
                )
            }
        }
    }

    Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(R.string.label, Sort.LABEL, isNumberColumn = false)
        VerticalDivider()
        HeaderCell(
            R.string.budget_table_header_allocated,
            Sort.ALLOCATED,
            color = LocalColors.current.income.takeIf { showChart }
        )
        VerticalDivider()
        HeaderCell(
            R.string.budget_table_header_spent,
            Sort.SPENT,
            color = LocalColors.current.expense.takeIf { showChart }
        )
        VerticalDivider()
        HeaderCell(R.string.available, Sort.AVAILABLE)
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
    hasRolloverNext: Boolean,
    editRollOver: SnapshotStateMap<Long, Pair<Long, Boolean>>?,
    narrowScreen: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .labelColumn(this, narrowScreen)
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
        BudgetNumbers(
            category = category,
            currency = currency,
            onBudgetEdit,
            onShowTransactions,
            hasRolloverNext,
            editRollOver,
            narrowScreen
        )
    }
}

@Composable
private fun RowScope.BudgetNumbers(
    category: Category,
    currency: CurrencyUnit,
    onBudgetEdit: () -> Unit,
    onShowTransactions: () -> Unit,
    hasRolloverNext: Boolean,
    editRollOver: SnapshotStateMap<Long, Pair<Long, Boolean>>?,
    narrowScreen: Boolean
) {
    val totalBudget = category.budget.budget + category.budget.rollOverPrevious
    //Allocation
    val allocation =
        if (category.children.isEmpty()) totalBudget else
            category.children.sumOf { it.budget.budget }
    Column(modifier = Modifier.numberColumn(this, narrowScreen)) {
        AmountText(
            modifier = Modifier
                .testTag(TEST_TAG_BUDGET_BUDGET)
                .clickable(onClick = onBudgetEdit)
                .fillMaxWidth(),
            amount = category.budget.budget,
            currency = currency,
            textAlign = TextAlign.End,
            textDecoration = TextDecoration.Underline
        )
        if (category.budget.rollOverPrevious != 0L) {
            ColoredAmountText(
                modifier = Modifier.fillMaxWidth(),
                amount = category.budget.rollOverPrevious,
                currency = currency,
                textAlign = TextAlign.End,
                prefix = if (category.budget.rollOverPrevious > 0) "+" else ""
            )
            AmountText(
                modifier = Modifier.fillMaxWidth(),
                prefix = " = ",
                amount = totalBudget,
                currency = currency,
                textAlign = TextAlign.End
            )
        }
        if (allocation != category.budget.totalAllocated && allocation != 0L) {
            val isError = allocation > category.budget.totalAllocated
            val errorIndication = if (isError) "!" else ""
            AmountText(
                modifier = Modifier
                    .testTag(TEST_TAG_BUDGET_ALLOCATION)
                    .fillMaxWidth(),
                prefix = "$errorIndication(",
                postfix = ")$errorIndication",
                amount = allocation,
                currency = currency,
                textAlign = TextAlign.End,
                color = if (isError)
                    colorResource(id = R.color.colorErrorDialog) else Color.Unspecified
            )
        }
    }

    VerticalDivider()

    //Spent
    val aggregateSum = category.aggregateSum
    AmountText(
        modifier = Modifier
            .testTag(TEST_TAG_BUDGET_SPENT)
            .numberColumn(this, narrowScreen)
            .clickable(onClick = onShowTransactions),
        amount = aggregateSum,
        currency = currency,
        textAlign = TextAlign.End,
        textDecoration = TextDecoration.Underline
    )

    VerticalDivider()

    //Remainder
    val remainder = category.budget.totalAllocated + aggregateSum
    ColoredAmountText(
        modifier = Modifier.numberColumn(this, narrowScreen),
        amount = remainder,
        currency = currency,
        textAlign = TextAlign.End,
        withBorder = true
    )

    //Rollover
    if (hasRolloverNext || editRollOver != null) {
        VerticalDivider()
        val rollOverFromChildren =
            category.aggregateRollOverNext(editRollOver?.mapValues { it.value.first })
        Column(
            modifier = Modifier.numberColumn(this, narrowScreen),
            horizontalAlignment = Alignment.End
        ) {
            if (editRollOver != null && (remainder != 0L)) {
                val rollOver =
                    editRollOver.getOrDefault(category.id, category.budget.rollOverNext to false)
                val rollOverTotal = rollOver.first + rollOverFromChildren
                val isError =
                    rollOverTotal.sign * remainder.sign == -1 || rollOverTotal.absoluteValue > remainder.absoluteValue
                editRollOver[category.id] = rollOver.first to isError

                AmountEdit(
                    value = Money(currency, rollOver.first).amountMajor,
                    onValueChange = {
                        val newRollOver = Money(currency, it).amountMinor
                        editRollOver[category.id] =
                            newRollOver to (newRollOver + rollOverFromChildren > remainder)
                    },
                    fractionDigits = currency.fractionDigits,
                    isError = isError
                )
            } else if (category.budget.rollOverNext != 0L) {
                ColoredAmountText(
                    amount = category.budget.rollOverNext,
                    currency = currency,
                    textAlign = TextAlign.End
                )
            }
            if (rollOverFromChildren != 0L) {
                ColoredAmountText(
                    amount = rollOverFromChildren,
                    currency = currency,
                    textAlign = TextAlign.End,
                    prefix = "(",
                    postfix = ")"
                )
            }
        }
    }
}

fun Category.aggregateRollOverNext(rollOverMap: Map<Long, Long>?): Long {
    return children.sumOf {
        (rollOverMap?.getOrDefault(it.id, it.budget.rollOverNext) ?: it.budget.rollOverNext) +
                it.aggregateRollOverNext(rollOverMap)
    }
}