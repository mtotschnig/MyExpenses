package org.totschnig.myexpenses.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutModel
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.FilterItem
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.util.formatMoney
import org.totschnig.myexpenses.util.ui.DisplayProgress
import org.totschnig.myexpenses.util.ui.displayProgress
import org.totschnig.myexpenses.viewmodel.data.Budget
import java.text.DecimalFormat
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Composable
fun ExpansionHandle(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    toggle: () -> Unit,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0F else 180F
    )
    IconButton(modifier = modifier, onClick = toggle) {
        Icon(
            modifier = Modifier.rotate(rotationAngle),
            imageVector = Icons.Default.ExpandLess,
            contentDescription = stringResource(
                id = if (isExpanded) R.string.collapse
                else R.string.expand
            )
        )
    }
}

@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    color: Int,
    content: @Composable BoxScope.() -> Unit = {},
) {
    ColorCircle(modifier, Color(color), content)
}

@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    color: Color,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
        content = content
    )
}

val generalPadding
    @Composable get() = dimensionResource(id = R.dimen.general_padding)

@Composable
fun DonutInABox(
    modifier: Modifier = Modifier,
    progress: Float,
    fontSize: TextUnit,
    color: Color,
    excessColor: Color,
) {

    Box(modifier = modifier) {
        DonutProgress(
            modifier = Modifier.fillMaxSize(),
            model = DonutModel(
                cap = 100f,
                gapWidthDegrees = 0f,
                gapAngleDegrees = 0f,
                strokeWidth = LocalContext.current.resources.getDimension(R.dimen.progress_donut_stroke_width),
                strokeCap = StrokeCap.Butt,
                sections = DisplayProgress.calcProgressVisualRepresentation(
                    progress.coerceAtLeast(
                        0f
                    )
                ).forCompose(color, excessColor)
            )
        )
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = progress.displayProgress ,
            fontSize = fontSize,
        )
    }
}

@Composable
fun TypeConfiguration(
    modifier: Modifier,
    typeFlags: Byte,
    onCheckedChange: (Byte) -> Unit,
) {
    MultiChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        val options = listOf(R.string.expense to FLAG_EXPENSE, R.string.income to FLAG_INCOME)
        options.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onCheckedChange = {
                    onCheckedChange(if (it) typeFlags or type.second else typeFlags and type.second.inv())
                },
                checked = (typeFlags and type.second) != 0u.toByte()
            ) {
                Text(stringResource(id = type.first))
            }
        }
    }
}

@Composable
fun CheckBoxWithLabel(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = label)
    }
}

@Composable
fun emToDp(em: Float): Dp = with(LocalDensity.current) {
    (LocalTextStyle.current.fontSize.takeIf { it.isSp } ?: 12.sp).toDp()
} * em

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SumDetails(
    incomeSum: Money,
    expenseSum: Money,
    transferSum: Money,
    alignStart: Boolean
) {
    val amountFormatter = LocalCurrencyFormatter.current

    FlowRow(
        modifier = Modifier
            .testTag(TEST_TAG_GROUP_SUMS)
            .fillMaxWidth(),
        horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.Center
    ) {
        Text(
            modifier = Modifier.amountSemantics(incomeSum),
            text = "⊕ " + amountFormatter.formatMoney(incomeSum),
            color = LocalColors.current.income
        )
        val configureExpenseSum: (DecimalFormat) -> Unit = remember {
            {
                it.negativePrefix = ""
                it.positivePrefix = "+"
            }
        }
        Text(
            modifier = Modifier
                .amountSemantics(expenseSum)
                .padding(horizontal = generalPadding),
            text = "⊖ " + amountFormatter.formatMoney(
                expenseSum,
                configureExpenseSum
            ),
            color = LocalColors.current.expense
        )
        Text(
            modifier = Modifier.amountSemantics(transferSum),
            text = Transfer.BI_ARROW + " " + amountFormatter.formatMoney(transferSum),
            color = LocalColors.current.transfer
        )
    }
}

@Composable
fun ChipGroup(
    modifier: Modifier = Modifier,
    budget: Budget?,
    criteria: List<Criterion<*>>,
) {
    val context = LocalContext.current
    ChipGroup(
        modifier,
        (budget?.let { listOf(it.label(context)) } ?: emptyList()) +
                criteria.map { it.prettyPrint(context) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    modifier: Modifier,
    chips: Iterable<String>
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        chips.forEach {
            FilterItem(it)
        }
    }
}

fun LazyListScope.simpleStickyHeader(text: String) {
    simpleStickyHeader {
        Text(text = text, modifier = it)
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.simpleStickyHeader(content: @Composable (Modifier) -> Unit) {
    stickyHeader {
        Surface(
            modifier = Modifier
                .height(32.dp)
            ,
            tonalElevation = 1.dp
        ) {
            content(
                Modifier.fillMaxWidth().padding(
                    horizontal = dimensionResource(R.dimen.padding_main_screen),
                    vertical = 4.dp
                )
            )
        }
    }
}