package org.totschnig.myexpenses.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.futured.donut.compose.DonutProgress
import app.futured.donut.compose.data.DonutModel
import app.futured.donut.compose.data.DonutSection
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Composable
fun ExpansionHandle(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    toggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0F else 180F
    )
    IconButton(modifier = modifier, onClick = toggle) {
        Icon(
            modifier = Modifier.rotate(rotationAngle),
            imageVector = Icons.Default.ExpandLess,
            contentDescription = stringResource(
                id = if (isExpanded) R.string.content_description_collapse
                else R.string.content_description_expand
            )
        )
    }
}

@Composable
fun ColorCircle(modifier: Modifier = Modifier, color: Int, content: @Composable BoxScope.() -> Unit = {}) {
    ColorCircle(modifier , Color(color), content)
}

@Composable
fun ColorCircle(modifier: Modifier = Modifier, color: Color, content: @Composable BoxScope.() -> Unit = {}) {
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
    modifier: Modifier,
    progress: Int,
    fontSize: TextUnit,
    strokeWidth: Float = 15f,
    color: Color
) {
    Box(modifier = modifier) {
        DonutProgress(
            modifier = Modifier.fillMaxSize(),
            model = DonutModel(
                cap = 100f,
                masterProgress = 1f,
                gapWidthDegrees = 0f,
                gapAngleDegrees = 0f,
                strokeWidth = strokeWidth,
                sections = listOf(DonutSection(amount = progress.toFloat(), color = color))
            )
        )
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = progress.toString(),
            fontSize = fontSize,
            )
    }
}

// Swap this in when upgrading to 1.6
/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeConfiguration(
    modifier: Modifier,
    typeFlags: Byte,
    onCheckedChange: (Byte) -> Unit
) {
    MultiChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        val options = listOf(R.string.expense to FLAG_EXPENSE, R.string.income to FLAG_INCOME)
        options.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onCheckedChange = {
                    onCheckedChange(if(it) typeFlags or type.second else typeFlags and type.second.inv())
                },
                checked = (typeFlags and type.second) != 0u.toByte()
            ) {
                Text(stringResource(id = type.first))
            }
        }
    }
}*/

@Composable
fun TypeConfiguration(
    modifier: Modifier,
    typeFlags: Byte,
    onCheckedChange: (Byte) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val options = listOf(R.string.expense to FLAG_EXPENSE, R.string.income to FLAG_INCOME)
        options.forEach { type ->
            Checkbox(
                onCheckedChange = {
                    onCheckedChange(if(it) typeFlags or type.second else typeFlags and type.second.inv())
                },
                checked = (typeFlags and type.second) != 0.toByte()
            )
            Text(stringResource(id = type.first))
        }
    }
}

@Preview
@Composable
fun ConditionalTest() {
    Text(
        modifier = Modifier
            .padding(15.dp)
            .border(width = 2.dp, color = Color.Blue)
            .conditional(true) {
                background(Color.Red)
            }
            .padding(16.dp),
        text = "Text"
    )
}