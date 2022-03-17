package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun Category(
    modifier: Modifier = Modifier,
    category: Category,
    expansionState: SnapshotStateList<Long>,
    selectionState: SnapshotStateList<Long>,
    onEdit: (Category) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onToggleSelection: (Long) -> Unit = {}
) {
    Column(modifier = modifier) {

        if (category.level > 0) {
            CategoryRenderer(
                category = category,
                expansionState = expansionState,
                selectionState = selectionState,
                onEdit = { onEdit(category) },
                onDelete = { onDelete(category.id) },
                onToggleSelection = { onToggleSelection(category.id) }
            )
            AnimatedVisibility(visible = expansionState.contains(category.id)) {
                Column(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    category.children.forEach { model ->
                        Category(
                            category = model,
                            expansionState = expansionState,
                            selectionState = selectionState,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onToggleSelection = onToggleSelection
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                category.children.forEach { model ->
                    item {
                        Category(
                            category = model,
                            expansionState = expansionState,
                            selectionState = selectionState,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onToggleSelection = onToggleSelection
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryRenderer(
    category: Category,
    expansionState: SnapshotStateList<Long>,
    selectionState: SnapshotStateList<Long>,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleSelection: () -> Unit
) {
    val isExpanded = expansionState.contains(category.id)
    val isSelected = selectionState.contains(category.id)
    val showMenu = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .then(if (isSelected) Modifier.background(Color.LightGray) else Modifier)
            .combinedClickable(
                onLongClick = onToggleSelection,
                onClick = {
                    if (selectionState.size == 0) {
                        showMenu.value = true
                    } else {
                        onToggleSelection()
                    }
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.children.isEmpty()) {
            Spacer(modifier = Modifier.width(24.dp))
        } else {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = {
                        expansionState.toggle(category.id)
                    }),
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    id = if (isExpanded)
                        R.string.content_description_collapse else
                        R.string.content_description_expand
                )
            )
        }
        if (category.icon != null) {
            val context = LocalContext.current
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(
                    id = context.resources.getIdentifier(
                        category.icon,
                        "drawable",
                        context.packageName
                    )
                ),
                contentDescription = null
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Text(text = category.label)
        if (category.level == 1 && category.color != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(category.color))
            )
        }
        HierarchicalMenu(
            showMenu, Menu(
                listOf(
                    MenuEntry(label = stringResource(id = R.string.menu_edit), action = onEdit),
                    MenuEntry(label = stringResource(id = R.string.menu_delete), action = onDelete),
                )
            )
        )
    }
}

@Preview(heightDp = 300)
@Composable
fun TreePreview() {
    var counter = 0;
    fun buildCategory(
        color: Int?,
        nrOfChildren: Int,
        childColors: List<Int>?,
        level: Int
    ): Category {
        val id = counter++
        return Category(
            id = counter.toLong(),
            level = level,
            label = "_$id",
            children = buildList {
                repeat(nrOfChildren) {
                    add(
                        buildCategory(
                            childColors?.get(it % childColors.size),
                            if (nrOfChildren == 1) 0 else floor(sqrt(nrOfChildren.toFloat())).toInt(),
                            null,
                            level + 1
                        )
                    )
                }
                add(Category(label = "BOGUS", level = level + 1))
            },
            color = color
        )
    }

    Category(
        category = buildCategory(
            level = 0,
            color = null,
            nrOfChildren = 10,
            childColors = listOf(
                android.graphics.Color.RED,
                android.graphics.Color.GREEN,
                android.graphics.Color.BLUE
            )
        ),
        expansionState = remember { mutableStateListOf(0, 1, 2) },
        selectionState = remember { mutableStateListOf(3, 4, 5) }
    )
}

@Immutable
data class Category(
    val id: Long = 0,
    val level: Int = 0,
    val label: String,
    val children: List<Category> = emptyList(),
    val isMatching: Boolean = true,
    val color: Int? = null,
    val icon: String? = null
) {

    fun pruneNonMatching(): Category? {
        val prunedChildren = children.mapNotNull { it.pruneNonMatching() }
        return if (isMatching || prunedChildren.isNotEmpty()) {
            this.copy(children = prunedChildren)
        } else null
    }

    companion object {
        val EMPTY = Category(label = "EMPTY")
    }
}

