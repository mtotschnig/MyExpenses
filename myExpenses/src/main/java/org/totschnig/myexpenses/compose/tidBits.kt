package org.totschnig.myexpenses.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
fun ColorCircle(modifier: Modifier = Modifier, color: Int) {
    ColorCircle(modifier , Color(color))
}

@Composable
fun ColorCircle(modifier: Modifier = Modifier, color: Color) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(CircleShape)
            .background(color)
    )
}
fun Modifier.conditional(condition : Boolean, block : Modifier.() -> Modifier) = if (condition) {
        then(block(this))
    } else {
        this
    }

fun Modifier.conditionalComposed(condition : Boolean, block : @Composable Modifier.() -> Modifier)  =
    composed {
        if (condition) {
            then(block(this))
        } else {
            this
        }
    }