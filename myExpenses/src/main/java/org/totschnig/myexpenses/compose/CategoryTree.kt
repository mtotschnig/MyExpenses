package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    state: SnapshotStateList<String>,
    onEdit: (Category) -> Unit = {}
) {
    Column(modifier = modifier) {

        if (category.level > 0) {
            CategoryRenderer(
                category = category,
                state = state
            ) { onEdit(category) }
            AnimatedVisibility(visible = state.contains(category.label)) {
                Column(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    category.children.forEach { model ->
                        Category(
                            category = model,
                            state = state,
                            onEdit = onEdit
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
                            state = state,
                            onEdit = onEdit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRenderer(
    category: Category,
    state: SnapshotStateList<String>,
    onEdit: () -> Unit = {}
) {
    val isExpanded = state.contains(category.label)
    val showMenu = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.clickable { showMenu.value = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.children.isEmpty()) {
            Spacer(modifier = Modifier.width(24.dp))
        } else {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = {
                        if (isExpanded) state.remove(category.label) else state.add(category.label)
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
                    MenuEntry(label = stringResource(id = R.string.menu_edit), action = onEdit)
                )
            )
        )
    }
}

@Preview
@Composable
fun TreePreview() {
    fun buildCategory(
        id: String,
        color: Int?,
        nrOfChildren: Int,
        childColors: List<Int>?
    ): Category {
        return Category(
            label = id,
            children = buildList {
                repeat(nrOfChildren) {
                    add(
                        buildCategory(
                            "${id}_$it",
                            childColors?.get(it % childColors.size),
                            if (nrOfChildren == 1) 0 else floor(sqrt(nrOfChildren.toFloat())).toInt(),
                            null
                        )
                    )
                }
                add(Category.EMPTY)
            },
            color = color
        )
    }

    val state = remember { mutableStateListOf("Root_0", "Root_0_0", "Root_0_0_0", "Root_0_0_0_0") }
    Category(
        category = buildCategory(
            id = "Root",
            color = null,
            nrOfChildren = 10,
            childColors = listOf(
                android.graphics.Color.RED, android.graphics.Color.GREEN, android.graphics.Color.BLUE
            )
        ),
        state = state,
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
        val EMPTY = Category(label = "EMPTY", icon = "school")
    }
}

