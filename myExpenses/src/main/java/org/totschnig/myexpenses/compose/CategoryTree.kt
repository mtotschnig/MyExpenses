package org.totschnig.myexpenses.compose

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.material.composethemeadapter.MdcTheme
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun Category(
    modifier: Modifier = Modifier,
    category: Category,
    expansionMode: ExpansionMode,
    menuGenerator: @Composable (Category) -> Menu<Category>? = { null },
    selectedAncestor: Category? = null,
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
            CategoryRenderer(
                category = category,
                expansionMode = expansionMode,
                choiceMode = choiceMode,
                menuGenerator = menuGenerator,
                startPadding = startPadding,
                onToggleSelection = {
                    choiceMode.toggleSelection(selectedAncestor, category)
                },
                sumCurrency = sumCurrency
            )
        }
        if (category.level > 0) {
            AnimatedVisibility(visible = expansionMode.isExpanded(category.id)) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    filteredChildren.forEach { model ->
                        Category(
                            category = model,
                            expansionMode = expansionMode,
                            menuGenerator = menuGenerator,
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
        } else if (!withRoot || expansionMode.isExpanded(category.id)) {
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                itemsIndexed(filteredChildren) { index, item ->
                    Category(
                        category = item,
                        expansionMode = expansionMode,
                        menuGenerator = menuGenerator,
                        choiceMode = choiceMode,
                        excludedSubTree = excludedSubTree,
                        startPadding = subTreePadding,
                        sumCurrency = sumCurrency
                    )
                    if (index < filteredChildren.lastIndex) {
                        Divider()
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
    expansionMode: ExpansionMode,
    choiceMode: ChoiceMode,
    menuGenerator: @Composable (Category) -> Menu<Category>?,
    startPadding: Dp,
    onToggleSelection: () -> Unit,
    sumCurrency: CurrencyUnit?
) {
    val isExpanded = expansionMode.isExpanded(category.id)
    val showMenu = remember { mutableStateOf(false) }
    val menu = menuGenerator(category)
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
                            onClick = {
                                if (choiceMode.selectParentOnClick || category.children.isEmpty()) {
                                    onToggleSelection.invoke()
                                } else {
                                    expansionMode.toggle(category)
                                }
                            }
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
            .padding(end = 24.dp, start = startPadding)
            .semantics {
                if (isExpanded) {
                    collapse {
                        expansionMode.toggle(category)
                        true
                    }
                } else {
                    expand {
                        expansionMode.toggle(category)
                        true
                    }
                }
            },
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
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon)
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Text(text = category.label, modifier = Modifier.weight(1f))
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
            ColoredAmountText(
                modifier = Modifier.padding(start = 4.dp),
                amount = category.aggregateSum,
                currency = it,
            )
        }
        menu?.let {
            HierarchicalMenu(showMenu, menu, category)
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
    ): Category {
        val id = counter++
        return Category(
            id = counter,
            parentId = parentId,
            level = level,
            label = "Categories can have long names _$id",
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
                add(Category(label = "BOGUS", level = level + 1))
            },
            color = color
        )
    }
    MdcTheme {
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
            expansionMode = ExpansionMode.DefaultCollapsed(remember {
                mutableStateListOf(
                    0,
                    1,
                    2
                )
            }),
            choiceMode = ChoiceMode.SingleChoiceMode(remember { mutableStateOf(null) }),
            sumCurrency = CurrencyUnit.DebugInstance
        )
    }
}

interface ExpansionMode {
    fun isExpanded(id: Long): Boolean
    fun toggle(category: Category)
    abstract class MultiExpand(val state: SnapshotStateList<Long>) : ExpansionMode {
        override fun toggle(category: Category) {
            state.toggle(category.id)
        }
    }

    class DefaultExpanded(state: SnapshotStateList<Long>) : MultiExpand(state) {
        override fun isExpanded(id: Long) = !state.contains(id)
    }

    class DefaultCollapsed(state: SnapshotStateList<Long>) : MultiExpand(state) {
        override fun isExpanded(id: Long) = state.contains(id)
    }

    open class Single(val state: SnapshotStateList<Category>) : ExpansionMode {
        override fun isExpanded(id: Long) = state.any { it.id == id }
        override fun toggle(category: Category) {
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

    class SingleChoiceMode(
        val selectionState: MutableState<Category?>,
        val selectParentOnClick: Boolean = true,
        isSelectable: (Long) -> Boolean = { true }
    ) :
        ChoiceMode(false, isSelectable) {
        override fun isSelected(id: Long) = selectionState.value?.id == id
        override fun toggleSelection(selectedAncestor: Category?, category: Category) {
            selectionState.value = if (selectionState.value == category) null else category
        }
    }

    object NoChoice : ChoiceMode(false) {
        override fun isSelected(id: Long) = false

        override fun toggleSelection(selectedAncestor: Category?, category: Category) {}
    }
}

