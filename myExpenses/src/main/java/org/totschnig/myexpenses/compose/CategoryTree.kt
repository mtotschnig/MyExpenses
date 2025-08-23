package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.toggle
import org.totschnig.myexpenses.viewmodel.data.Category
import kotlin.math.floor
import kotlin.math.sqrt

val Byte?.typeBorderColor: Color
    @Composable get() = when (this) {
        FLAG_INCOME -> LocalColors.current.income
        FLAG_EXPENSE -> LocalColors.current.expense
        FLAG_TRANSFER -> LocalColors.current.transfer
        else -> Color.Transparent
    }

val Byte?.typeTextColor: Color
    @Composable get() = when (this) {
        FLAG_INCOME -> LocalColors.current.income
        FLAG_EXPENSE -> LocalColors.current.expense
        FLAG_TRANSFER -> LocalColors.current.transfer
        else -> Color.Unspecified
    }

@Composable
fun Category(
    modifier: Modifier = Modifier,
    category: Category,
    expansionMode: ExpansionMode,
    menuGenerator: (Category) -> Menu? = { null },
    selectedAncestor: Category? = null,
    choiceMode: ChoiceMode,
    excludedSubTree: Long? = null,
    withRoot: Boolean = false,
    startPadding: Dp = 0.dp,
    sumCurrency: CurrencyUnit? = null,
    withTypeColors: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState? = null
) {
    val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)

    Column(
        modifier = modifier
            .conditional(choiceMode.isTreeSelected(category.id)) {
                padding(2.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(activatedBackgroundColor)
            }
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
                sumCurrency = sumCurrency,
                withTypeColors = withTypeColors
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
                                ?: if (choiceMode.mainOnly || choiceMode.isSelected(category.id)) category else null,
                            choiceMode = choiceMode,
                            excludedSubTree = excludedSubTree,
                            startPadding = subTreePadding,
                            sumCurrency = sumCurrency,
                            withTypeColors = withTypeColors
                        )
                    }
                }
            }
        } else if (!withRoot || expansionMode.isExpanded(category.id)) {
            LazyColumn(
                modifier = Modifier
                    .testTag(TEST_TAG_LIST)
                    .semantics {
                        collectionInfo = CollectionInfo(filteredChildren.size, 1)
                    },
                verticalArrangement = Arrangement.Center,
                contentPadding = contentPadding,
                state = listState ?: rememberLazyListState()
            ) {
                // if we are passed in a collection of trees (i.e. the categories on the first level have level == 0)
                // we flatten this first level away. The trees are separated by a thicker divider
                // This is used in DistributionActivity to pass in income and expense categories together
                // in order to render them in one scrollable list
                filteredChildren.forEachIndexed { index1, category1 ->
                    if (category1.level == 0) {
                        category1.children.forEachIndexed { index2, category2 ->
                            item(category2.id) {
                                Category(
                                    category = category2,
                                    expansionMode = expansionMode,
                                    menuGenerator = menuGenerator,
                                    choiceMode = choiceMode,
                                    excludedSubTree = excludedSubTree,
                                    startPadding = subTreePadding,
                                    sumCurrency = sumCurrency,
                                    withTypeColors = withTypeColors,
                                )
                                HorizontalDivider(thickness = if (index2 == category1.children.lastIndex && index1 != filteredChildren.lastIndex) 2.dp else 1.dp)
                            }
                        }
                    } else {
                        item(category1.id) {
                            Category(
                                modifier = Modifier.semantics {
                                    collectionItemInfo = CollectionItemInfo(
                                        rowIndex = index1,
                                        columnIndex = 1,
                                        rowSpan = 1,
                                        columnSpan = 1
                                    )
                                },
                                category = category1,
                                expansionMode = expansionMode,
                                menuGenerator = menuGenerator,
                                choiceMode = choiceMode,
                                excludedSubTree = excludedSubTree,
                                startPadding = subTreePadding,
                                sumCurrency = sumCurrency,
                                withTypeColors = withTypeColors,
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryContentDescription(category: Category): String {
     val categoryTypeString = if (category.level == 1) {
        stringResource(id = R.string.category_type_main)
    } else {
        stringResource(id = R.string.category_type_sub)
    }

    var description = stringResource(
        id = R.string.category_description_base,
        categoryTypeString, // %1$s
        category.level      // %2$d
    )

    if (category.children.isNotEmpty()) {
        val childrenSuffix = pluralStringResource(
            id = R.plurals.category_description_children_suffix,
            count = category.children.size,
            category.children.size,
            category.level + 1
        )
        description += childrenSuffix
    }
    return description
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryRenderer(
    category: Category,
    expansionMode: ExpansionMode,
    choiceMode: ChoiceMode,
    menuGenerator: (Category) -> Menu?,
    startPadding: Dp,
    onToggleSelection: () -> Unit,
    sumCurrency: CurrencyUnit?,
    withTypeColors: Boolean
) {
    val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
    val isExpanded = expansionMode.isExpanded(category.id)
    val showMenu = rememberSaveable { mutableStateOf(false) }
    val menu = menuGenerator(category)
    val contentDescription = getCategoryContentDescription(category)
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .conditional(choiceMode.isSelectable(category.id)) {
                if (menu == null) {
                    clickable(onClick = onToggleSelection)
                } else when (choiceMode) {
                    is ChoiceMode.MultiChoiceMode ->
                        combinedClickable(
                            onLongClick = onToggleSelection,
                            onClick = {
                                if (choiceMode.selectionState.isEmpty()) {
                                    showMenu.value = true
                                } else {
                                    onToggleSelection()
                                }
                            }
                        )

                    is ChoiceMode.SingleChoiceMode ->
                        combinedClickable(
                            onLongClick = { showMenu.value = true },
                            onClick = {
                                if (choiceMode.selectParentOnClick || category.children.isEmpty()) {
                                    onToggleSelection()
                                } else {
                                    expansionMode.toggle(category)
                                }
                            }
                        )

                    else -> Modifier
                }
            }
            .conditional(choiceMode.isNodeSelected(category.id)) {
                background(activatedBackgroundColor)
                    .semantics { selected = true }
            }
            .padding(end = 24.dp, start = startPadding)
            .conditional(category.children.isNotEmpty()) {
                semantics {
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
                }
            }
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.children.isEmpty()) {
            Spacer(modifier = Modifier.width(48.dp))
        } else {
            ExpansionHandle(
                isExpanded = isExpanded
            ) {
                expansionMode.toggle(category)
            }
        }
        if (category.icon != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center
            ) {

                Icon(icon = category.icon, color = category.color?.let { Color(it) })
            }
        } else if (category.color != null) {
            ColorCircle(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(24.dp),
                color = category.color
            )
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }
        Text(
            text = category.label,
            modifier = Modifier.weight(1f),
            color = category.typeFlags.takeIf { withTypeColors }.typeTextColor
        )

        sumCurrency?.let {
            ColoredAmountText(
                modifier = Modifier.padding(start = 4.dp),
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
private fun TreePreview() {
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
    AppTheme {
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
    private val selectTree: Boolean,
    /**
     * if true, only main categories can be selected
     */
    val mainOnly: Boolean = false,

    val isSelectable: (Long) -> Boolean = { true }
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
        selectTree: Boolean = false,
        mainOnly: Boolean = false,
        isSelectable: (Long) -> Boolean = { true }
    ) : ChoiceMode(selectTree, mainOnly, isSelectable) {
        override fun isSelected(id: Long) = selectionState.value?.id == id
        override fun toggleSelection(selectedAncestor: Category?, category: Category) {
            (selectedAncestor?.takeIf { mainOnly } ?: category).let {
                selectionState.value = if (selectionState.value == it) null else it
            }
        }
    }

    data object NoChoice : ChoiceMode(false, isSelectable = { false }) {
        override fun isSelected(id: Long) = false

        override fun toggleSelection(selectedAncestor: Category?, category: Category) {}
    }
}

