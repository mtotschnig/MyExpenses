package org.totschnig.myexpenses.compose

import android.os.Parcelable
import androidx.appcompat.content.res.AppCompatResources
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun Category(
    modifier: Modifier = Modifier,
    category: Category,
    expansionState: SnapshotStateList<Long>?,
    onEdit: (Category) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onAdd: (Long) -> Unit = {},
    onMove: (Category) -> Unit = {},
    selectedAncestor: Category? = null,
    choiceMode: ChoiceMode
) {
    Column(modifier = modifier.then(if (choiceMode.isTreeSelected(category.id)) Modifier.background(Color.LightGray) else Modifier)) {
        if (category.level > 0) {
            CategoryRenderer(
                category = category,
                expansionState = expansionState,
                choiceMode = choiceMode,
                onEdit = { onEdit(category) },
                onDelete = { onDelete(category.id) },
                onAdd = { onAdd(category.id) },
                onMove = { onMove(category) },
                onToggleSelection = {
                    choiceMode.toggleSelection(selectedAncestor, category)
                }
            )
            AnimatedVisibility(visible = expansionState?.contains(category.id) ?: true) {
                Column(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    category.children.forEach { model ->
                        Category(
                            category = model,
                            expansionState = expansionState,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onAdd = onAdd,
                            onMove = onMove,
                            selectedAncestor = selectedAncestor
                                ?: if (choiceMode.isSelected(category.id)) category else null,
                            choiceMode = choiceMode
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
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onAdd = onAdd,
                            onMove = onMove,
                            choiceMode = choiceMode
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
    expansionState: SnapshotStateList<Long>?,
    choiceMode: ChoiceMode,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onAdd: () -> Unit = {},
    onMove: () -> Unit = {},
    onToggleSelection: () -> Unit
) {
    val isExpanded = expansionState?.contains(category.id) ?: true
    val showMenu = remember { mutableStateOf(false) }
    Row(
        modifier = when(choiceMode) {
            is ChoiceMode.MultiChoiceMode -> Modifier
                .combinedClickable(
                    onLongClick = onToggleSelection,
                    onClick = {
                        if (choiceMode.selectionState.size == 0) {
                            showMenu.value = true
                        } else {
                            onToggleSelection()
                        }
                    }
                )
            is ChoiceMode.SingleChoiceMode -> Modifier.clickable(onClick = onToggleSelection)
        }.then(if (choiceMode.isNodeSelected(category.id)) Modifier.background(Color.LightGray) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.children.isEmpty()) {
            Spacer(modifier = Modifier.width(24.dp))
        } else {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (expansionState == null) Modifier else
                            Modifier.clickable(onClick = {
                                expansionState.toggle(category.id)
                            })
                    ),
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
            val drawable = AppCompatResources.getDrawable(
                context,
                context.resources.getIdentifier(category.icon, "drawable", context.packageName)
            )
            Icon(
                modifier = Modifier.size(24.dp),
                painter = rememberDrawablePainter(drawable = drawable),
                contentDescription = category.icon
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
                    MenuEntry.edit(onEdit),
                    MenuEntry.delete(onDelete),
                    MenuEntry(
                        icon = Icons.Filled.Add,
                        label = stringResource(id = R.string.subcategory),
                        action = onAdd
                    ),
                    MenuEntry(
                        icon = myiconpack.ArrowsAlt,
                        label = stringResource(id = R.string.menu_move),
                        action = onMove
                    )
                )
            )
        )
    }
}

@Preview(heightDp = 300)
@Composable
fun TreePreview() {
    var counter = 0
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
        choiceMode = ChoiceMode.SingleChoiceMode(remember { mutableStateOf(null) }, false)
    )
}

sealed class ChoiceMode(
    /**
     * if true, selecting a category highlights the tree (including children), if false children are
     * not highlighted
     */
    val selectTree: Boolean
) {
    fun isTreeSelected(id: Long) = selectTree && isSelected(id)
    fun isNodeSelected(id: Long) = !selectTree && isSelected(id)

    abstract fun isSelected(id: Long): Boolean

    abstract fun toggleSelection(selectedAncestor: Category?, category: Category)

    class MultiChoiceMode(val selectionState: SnapshotStateList<Long>, selectTree: Boolean) :
        ChoiceMode(selectTree) {
        override fun isSelected(id: Long) = selectionState.contains(id)
        override fun toggleSelection(selectedAncestor: Category?, category: Category) {
            (selectedAncestor ?: category).let {
                if (selectionState.toggle(it.id)) {
                    //when we select a category, children are implicitly selected, so we remove
                    //them from the explicit selection
                    it.recursiveUnselectChildren(selectionState)
                }
            }
        }
    }

    class SingleChoiceMode(val selectionState: MutableState<Category?>, selectTree: Boolean) :
        ChoiceMode(selectTree) {
        override fun isSelected(id: Long) = selectionState.value?.id == id
        override fun toggleSelection(selectedAncestor: Category?, category: Category) {
            selectionState.value = if (selectionState.value == category) null else category
        }
    }
}

@Immutable
@Parcelize
data class Category(
    val id: Long = 0,
    val level: Int = 0,
    val label: String,
    val children: List<Category> = emptyList(),
    val isMatching: Boolean = true,
    val color: Int? = null,
    val icon: String? = null
) : Parcelable {

    fun pruneNonMatching(): Category? {
        val prunedChildren = children.mapNotNull { it.pruneNonMatching() }
        return if (isMatching || prunedChildren.isNotEmpty()) {
            this.copy(children = prunedChildren)
        } else null
    }

    fun recursiveUnselectChildren(selectionState: SnapshotStateList<Long>) {
        children.forEach {
            selectionState.remove(it.id)
            it.recursiveUnselectChildren(selectionState)
        }
    }

    companion object {
        val EMPTY = Category(label = "EMPTY")
    }
}

