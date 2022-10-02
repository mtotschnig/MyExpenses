package org.totschnig.myexpenses.compose

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

fun Modifier.conditional(condition : Boolean, block : Modifier.() -> Modifier) = if (condition) {
        then(block(Modifier))
    } else {
        this
    }

@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.conditionalComposed(condition : Boolean, block : @Composable Modifier.() -> Modifier)  =
    composed {
        if (condition) {
            then(block(Modifier))
        } else {
            this
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