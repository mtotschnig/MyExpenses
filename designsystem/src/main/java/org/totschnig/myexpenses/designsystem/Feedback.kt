package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * How strongly a [Banner] stands out
 */
enum class BannerStyle { Question, Alert }

/**
 * Tinted card that asks for a decision or tells about something new, with actions at the bottom
 *
 * @param messages one paragraph each
 */
@Composable
fun Banner(
    title: String,
    messages: List<String>,
    modifier: Modifier = Modifier,
    style: BannerStyle = BannerStyle.Question,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val (color, contentColor) = when (style) {
        BannerStyle.Question -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        BannerStyle.Alert -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = Shapes.card,
        color = color,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.content)
    ) {
        Column(
            Modifier.padding(
                start = Spacing.content, end = Spacing.content, top = Spacing.content,
                bottom = if (actions != null) 8.dp else Spacing.content
            )
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            messages.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            if (actions != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

/**
 * Shown instead of an empty list
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Card in the middle of an otherwise empty page that asks the user to start something,
 * e.g. to allow the analysis of transactions
 *
 * @param dismissLabel label of a second button below, e.g. "Not now", none if null
 */
@Composable
fun PromptCard(
    icon: ImageVector,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.screen),
        contentAlignment = Alignment.Center
    ) {
        ActionCard(icon, title, text, confirmLabel, onConfirm, dismissLabel = dismissLabel, onDismiss = onDismiss)
    }
}

/**
 * Card within a page that asks the user to start something, e.g. to add a first account,
 * see [PromptCard] for a page that is otherwise empty
 *
 * @param confirmIcon shown in the button before [confirmLabel]
 * @param dismissLabel label of a second button below, e.g. "Not now", none if null
 */
@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmIcon: ImageVector? = null,
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {},
) {
    Surface(
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onConfirm,
                contentPadding = if (confirmIcon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                if (confirmIcon != null) {
                    Icon(confirmIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                }
                Text(confirmLabel)
            }
            if (dismissLabel != null) {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        }
    }
}

/**
 * A small card within a page that invites to start something optional, e.g. a first budget.
 * Unlike an [ActionCard] it takes only one line, and the user can close it for good.
 *
 * @param onClick starts it
 * @param onDismiss closes the card, the caller remembers that
 */
@Composable
fun InviteCard(
    icon: ImageVector,
    title: String,
    text: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.content, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = Spacing.content)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = dismissLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Scrolls its content into view and lets it light up for a moment, e.g. a setting found by the
 * search
 *
 * @param onShown called when the light has faded, so that it is not shown again
 */
@Composable
fun Highlighted(isActive: Boolean, onShown: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    val light = remember { Animatable(0f) }
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        // once it is laid out
        withFrameNanos { }
        requester.bringIntoView()
        light.snapTo(1f)
        delay(600)
        light.animateTo(0f, tween(durationMillis = 1200))
        onShown()
    }
    Column(
        Modifier
            .bringIntoViewRequester(requester)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f * light.value)),
        content = content
    )
}
