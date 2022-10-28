package org.totschnig.myexpenses.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
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
        androidx.compose.material.Icon(
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