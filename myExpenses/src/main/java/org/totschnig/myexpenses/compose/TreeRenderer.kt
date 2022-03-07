package org.totschnig.myexpenses.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun Category(
    nodeModel: Category,
    state: SnapshotStateList<String>,
    isRoot: Boolean
) {
    Column(
        modifier =
        if (isRoot) Modifier else Modifier.padding(start = 24.dp)
    ) {

        if (!isRoot) {
            CategoryRenderer(
                category = nodeModel,
                state = state
            )
        }

        if (isRoot) {
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                nodeModel.children.forEach { model ->
                    item {
                        Category(
                            nodeModel = model,
                            state = state,
                            isRoot = false
                        )
                    }
                }
            }
        } else {
            AnimatedVisibility(visible = isRoot || state.contains(nodeModel.label)) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    nodeModel.children.forEach { model ->
                        Category(
                            nodeModel = model,
                            state = state,
                            isRoot = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRenderer(category: Category, state: SnapshotStateList<String>) {
    val isExpanded = state.contains(category.label)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (category.children.isNotEmpty()) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = {
                        if (isExpanded) state.remove(category.label) else state.add(category.label)
                    }),
                imageVector = if (isExpanded) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline,
                contentDescription = "Icon",
            )
        }
        Text(text = category.label)
    }
}

@Preview
@Composable
fun TreePreview() {
    fun buildCategory(id: String, nrOfChildren: Int): Category {
        return Category(
            label = id,
            children = buildList {
                repeat(nrOfChildren) {
                    add(buildCategory("${id}_$it", if (nrOfChildren == 1) 0 else floor(sqrt(nrOfChildren.toFloat())).toInt()))
                }
                add(Category.EMPTY)
            }
        )
    }

    val state = remember { mutableStateListOf("Root_0", "Root_0_0", "Root_0_0_0", "Root_0_0_0_0") }
    Category(
        nodeModel = buildCategory("Root", 10),
        state = state,
        isRoot = true
    )
}

@Immutable
class Category(val label: String, val children: List<Category>) {
    companion object {
       val EMPTY = Category("EMPTY", emptyList())
    }
}

