package org.totschnig.myexpenses.compose

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.Category2
import kotlin.math.floor
import kotlin.math.sqrt

typealias CategoryAction = ((Category2) -> Unit)

data class CategoryMenu(
    val onEdit: CategoryAction,
    val onDelete: CategoryAction,
    val onAdd: CategoryAction,
    val onMove: CategoryAction
)

@Composable
fun Category(
    modifier: Modifier = Modifier,
    category: Category2,
    expansionMode: ExpansionMode,
    menu: CategoryMenu? = null,
    selectedAncestor: Category2? = null,
    choiceMode: ChoiceMode,
    excludedSubTree: Long? = null,
    withRoot: Boolean = false,
    startPadding: Dp = 0.dp,
    sumCurrency: CurrencyUnit? = null
) {
    Column(
        modifier = modifier.then(
            if (choiceMode.isTreeSelected(category.id)) Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    colorResource(id = R.color.activatedBackground)
                ) else Modifier
        )
    ) {
        val filteredChildren =
            if (excludedSubTree == null) category.children else category.children.filter { it.id != excludedSubTree }
        val subTreePadding = startPadding + 12.dp
        if (withRoot || category.level > 0) {
            val menuEntries = if (menu != null) Menu(buildList {
                add(MenuEntry.edit { menu.onEdit(category) })
                add(MenuEntry.delete { menu.onDelete(category) })
                add(MenuEntry(
                    icon = Icons.Filled.Add,
                    label = stringResource(id = R.string.subcategory)
                ) { menu.onAdd(category) })
                add(MenuEntry(
                    icon = myiconpack.ArrowsAlt,
                    label = stringResource(id = R.string.menu_move)
                ) { menu.onMove(category) })
            }) else null
            CategoryRenderer(
                category = category,
                expansionMode = expansionMode,
                choiceMode = choiceMode,
                menu = menuEntries,
                startPadding = startPadding,
                onToggleSelection = {
                    choiceMode.toggleSelection(selectedAncestor, category)
                },
                sumCurrency = sumCurrency
            )
            AnimatedVisibility(visible = expansionMode.isExpanded(category.id)) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    filteredChildren.forEach { model ->
                        Category(
                            category = model,
                            expansionMode = expansionMode,
                            menu = menu,
                            selectedAncestor = selectedAncestor
                                ?: if (choiceMode.isSelected(category.id)) category else null,
                            choiceMode = choiceMode,
                            excludedSubTree = excludedSubTree,
                            startPadding = subTreePadding,
                            sumCurrency = sumCurrency
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                filteredChildren.forEach { model ->
                    item {
                        Category(
                            category = model,
                            expansionMode = expansionMode,
                            menu = menu,
                            choiceMode = choiceMode,
                            excludedSubTree = excludedSubTree,
                            startPadding = subTreePadding,
                            sumCurrency = sumCurrency
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
    category: Category2,
    expansionMode: ExpansionMode,
    choiceMode: ChoiceMode,
    menu: Menu?,
    startPadding: Dp,
    onToggleSelection: () -> Unit,
    sumCurrency: CurrencyUnit?
) {
    val isExpanded = expansionMode.isExpanded(category.id)
    val showMenu = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .then(if (menu == null) {

                if (choiceMode.isSelectable(category.id)) Modifier.clickable(
                    onClick = onToggleSelection
                ) else Modifier
            } else {

                when (choiceMode) {
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
                    is ChoiceMode.SingleChoiceMode -> Modifier
                        .combinedClickable(
                            onLongClick = { showMenu.value = true },
                            onClick = onToggleSelection
                        )
                    else -> Modifier
                }
            }
            )
            .then(
                if (choiceMode.isNodeSelected(category.id))
                    Modifier.background(colorResource(id = R.color.activatedBackground))
                else Modifier
            )
            .padding(end = 24.dp, start = startPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.children.isEmpty()) {
            Spacer(modifier = Modifier.width(48.dp))
        } else {
            IconButton(onClick = { expansionMode.toggle(category) }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        id = if (isExpanded)
                            R.string.content_description_collapse else
                            R.string.content_description_expand
                    )
                )
            }
        }
        if (category.icon != null) {
            val context = LocalContext.current
            val drawable = AppCompatResources.getDrawable(
                context,
                context.resources.getIdentifier(category.icon, "drawable", context.packageName)
            )
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 6.dp),
                painter = rememberDrawablePainter(drawable = drawable),
                contentDescription = category.icon
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Text(text = category.label)
        if (category.color != null) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(24.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(category.color))
            )
        }
        sumCurrency?.let {
            Spacer(modifier = Modifier.weight(1f))
            ColoredAmountText(
                amount = category.aggregateSum,
                currency = it,
            )
        }
        menu?.let {
            HierarchicalMenu(showMenu, menu)
        }
    }
}

@Preview(heightDp = 300)
@Composable
fun TreePreview() {
    var counter = 0L
    fun buildCategory(
        color: Int?,
        nrOfChildren: Int,
        childColors: List<Int>?,
        level: Int,
        parentId: Long?
    ): Category2 {
        val id = counter++
        return Category2(
            id = counter,
            parentId = parentId,
            level = level,
            label = "_$id",
            children = buildList {
                repeat(nrOfChildren) {
                    add(
                        buildCategory(
                            childColors?.get(it % childColors.size),
                            if (nrOfChildren == 1) 0 else floor(sqrt(nrOfChildren.toFloat())).toInt(),
                            null,
                            level + 1,
                            counter
                        )
                    )
                }
                add(Category2(label = "BOGUS", level = level + 1))
            },
            color = color
        )
    }

    Category(
        category = buildCategory(
            color = null,
            nrOfChildren = 10,
            childColors = listOf(
                android.graphics.Color.RED,
                android.graphics.Color.GREEN,
                android.graphics.Color.BLUE
            ),
            level = 0,
            parentId = null
        ),
        expansionMode = ExpansionMode.DefaultCollapsed(remember { mutableStateListOf(0, 1, 2) }),
        choiceMode = ChoiceMode.SingleChoiceMode(remember { mutableStateOf(null) })
    )
}

interface ExpansionMode {
    fun isExpanded(id: Long): Boolean
    fun toggle(category: Category2)
    abstract class MultiExpand(val state: SnapshotStateList<Long>) : ExpansionMode {
        override fun toggle(category: Category2) {
            state.toggle(category.id)
        }
    }

    class DefaultExpanded(state: SnapshotStateList<Long>) : MultiExpand(state) {
        override fun isExpanded(id: Long) = !state.contains(id)
    }

    class DefaultCollapsed(state: SnapshotStateList<Long>) : MultiExpand(state) {
        override fun isExpanded(id: Long) = state.contains(id)
    }

    open class Single(val state: SnapshotStateList<Category2>) : ExpansionMode {
        override fun isExpanded(id: Long) = state.any { it.id == id }
        override fun toggle(category: Category2) {
            val isExpanded = isExpanded(category.id)
            state.removeRange(state.indexOfFirst { it.id == category.parentId } + 1, state.size)
            if (!isExpanded) {
                state.add(category)
            }
        }
    }
}

sealed class ChoiceMode(
    /**
     * if true, selecting a category highlights the tree (including children), if false children are
     * not highlighted
     */
    private val selectTree: Boolean, val isSelectable: (Long) -> Boolean = { true }
) {
    fun isTreeSelected(id: Long) = selectTree && isSelected(id)
    fun isNodeSelected(id: Long) = !selectTree && isSelected(id)

    abstract fun isSelected(id: Long): Boolean

    abstract fun toggleSelection(selectedAncestor: Category2?, category: Category2)

    class MultiChoiceMode(val selectionState: SnapshotStateList<Long>, selectTree: Boolean) :
        ChoiceMode(selectTree) {
        override fun isSelected(id: Long) = selectionState.contains(id)
        override fun toggleSelection(selectedAncestor: Category2?, category: Category2) {
            (selectedAncestor ?: category).let {
                if (selectionState.toggle(it.id)) {
                    //when we select a category, children are implicitly selected, so we remove
                    //them from the explicit selection
                    it.recursiveUnselectChildren(selectionState)
                }
            }
        }
    }

    class SingleChoiceMode(
        val selectionState: MutableState<Category2?>,
        isSelectable: (Long) -> Boolean = { true }
    ) :
        ChoiceMode(false, isSelectable) {
        override fun isSelected(id: Long) = selectionState.value?.id == id
        override fun toggleSelection(selectedAncestor: Category2?, category: Category2) {
            selectionState.value = if (selectionState.value == category) null else category
        }
    }

}

