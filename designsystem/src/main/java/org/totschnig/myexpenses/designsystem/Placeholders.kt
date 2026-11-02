package org.totschnig.myexpenses.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pulsing block in place of content that is still loading. Keeps the place of the content, so that
 * nothing below it jumps when the content arrives.
 *
 * @param height of the content it stands for, e.g. a line of text
 */
@Composable
fun LoadingBlock(modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val alpha by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "loading alpha"
    )
    Box(
        modifier
            .height(height)
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, Shapes.badge)
    )
}

/**
 * Row of a card whose content is still loading, as high as a [LinkRow] or [InfoRow]
 */
@Composable
fun LoadingRow(isFirst: Boolean = false) {
    if (!isFirst) RowDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.content, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // As high as a line of bodyLarge, the block as high as its letters
        Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
            LoadingBlock(Modifier.fillMaxWidth(0.6f))
        }
    }
}
