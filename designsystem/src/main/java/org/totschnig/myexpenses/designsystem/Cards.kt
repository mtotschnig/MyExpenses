package org.totschnig.myexpenses.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A card holding rows, e.g. [InfoRow]s of a detail page
 */
@Composable
fun DetailCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

/**
 * Card of the overview: a small title above the content
 *
 * @param onClick opens the details, null for a card that cannot be opened
 * @param actionLabel of a link next to the title, e.g. "Show all", which calls [onAction]
 * @param edgeToEdge the content reaches the sides of the card, e.g. a row that scrolls sideways
 * and pads its items itself
 */
@Composable
fun OverviewCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    edgeToEdge: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(vertical = Spacing.content, horizontal = if (edgeToEdge) 0.dp else Spacing.content)) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .padding(horizontal = if (edgeToEdge) Spacing.content else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (actionLabel != null) {
                    Row(
                        modifier = Modifier
                            .clip(Shapes.chip)
                            .clickable(onClick = onAction)
                            .heightIn(min = 48.dp)
                            .padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            content()
        }
    }
    val color = MaterialTheme.colorScheme.surfaceContainerLowest
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = Shapes.card, color = color, content = body)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = Shapes.card, color = color, content = body)
    }
}

/**
 * An item of a list whose items of a group look like one card: rounded outer corners
 * and dividers between the items
 *
 * @param color e.g. to mark the item as selected
 * @param isLifted while the item is dragged: a card of its own with a shadow
 */
@Composable
fun GroupItem(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    isLifted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val elevation by animateDpAsState(if (isLifted) 6.dp else 0.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = if (isLifted) Shapes.card else Shapes.groupItem(position),
        color = color,
        shadowElevation = elevation
    ) {
        Column {
            if (!position.isFirst && !isLifted) RowDivider()
            content()
        }
    }
}

/**
 * Separates rows within a card
 */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer)
}

/**
 * Large title of a group of cards on a page, e.g. the accounts of the overview, with an optional
 * action on the right, e.g. to add an item. As high with or without the action.
 */
@Composable
fun GroupTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        // Vertical padding is reduced by the extra height of the 48dp touch target of the action
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        )
        action?.invoke()
    }
}

/**
 * Bold title above a card of a detail page
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.padding(start = 4.dp, top = Spacing.section, bottom = 8.dp)
    )
}

/**
 * The background of an icon that stands for an item: a circle in a list, a larger rounded square
 * in the header of its details
 */
@Composable
fun IconTile(
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(if (isLarge) 56.dp else 40.dp)
            .clip(if (isLarge) Shapes.tile else CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * The tile of a payee on an [IconTile]: its logo, else the first letter of its name on a color of
 * the theme that is always the same for the name, else [fallback]
 *
 * @param logo usually with a transparent background, shown on white so that dark logos stay
 * visible in the dark theme
 */
@Composable
fun PayeeTile(
    name: String?,
    logo: ImageBitmap?,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    fallback: @Composable BoxScope.() -> Unit,
) {
    val initial = name?.let(::monogramOf)
    when {
        logo != null -> IconTile(modifier, isLarge, color = Color.White) {
            Image(
                bitmap = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(if (isLarge) 12.dp else 8.dp)
            )
        }

        initial != null -> {
            val (container, content) = monogramColors(name)
            IconTile(modifier, isLarge, color = container) {
                Text(
                    text = initial,
                    color = content,
                    style = if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                )
            }
        }

        else -> IconTile(modifier, isLarge, content = fallback)
    }
}

/** The first letter or digit of [name] in upper case, null if it has none */
fun monogramOf(name: String): String? =
    name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()

/** One of the container colors of the theme, chosen by [name] so that it stays the same */
@Composable
private fun monogramColors(name: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val pairs = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.surfaceContainerHighest to scheme.onSurfaceVariant,
    )
    return pairs[Math.floorMod(name.trim().lowercase().hashCode(), pairs.size)]
}
