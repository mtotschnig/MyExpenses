package org.totschnig.myexpenses.compose

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun Category(
    nodeModel: Category,
    state: SnapshotStateList<String>,
    level: Int
) {
    Column {

        if (level > 0) {
            CategoryRenderer(
                category = nodeModel,
                state = state,
                level = level
            )
            AnimatedVisibility(visible = state.contains(nodeModel.label)) {
                Column(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    nodeModel.children.forEach { model ->
                        Category(
                            nodeModel = model,
                            state = state,
                            level = level + 1
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.Center
            ) {
                nodeModel.children.forEach { model ->
                    item {
                        Category(
                            nodeModel = model,
                            state = state,
                            level = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRenderer(category: Category, state: SnapshotStateList<String>, level: Int) {
    val isExpanded = state.contains(category.label)
    Row(verticalAlignment = Alignment.CenterVertically) {
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
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = category.icon),
                contentDescription = null
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(3.dp)
                    .then(
                        if (level == 1 && category.color != null) Modifier
                            .clip(CircleShape)
                            .background(category.color) else Modifier
                    )
            )
        }
        Text(text = category.label)
    }
}

@Preview
@Composable
fun TreePreview() {
    fun buildCategory(
        id: String,
        color: Color?,
        nrOfChildren: Int,
        childColors: List<Color>?
    ): Category {
        return Category(
            label = id,
            color = color,
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
            }
        )
    }

    val state = remember { mutableStateListOf("Root_0", "Root_0_0", "Root_0_0_0", "Root_0_0_0_0") }
    Category(
        nodeModel = buildCategory("Root", null, 10, listOf(Color.Red, Color.Green, Color.Blue)),
        state = state,
        level = 0
    )
}

@Immutable
class Category(
    val label: String,
    val color: Color? = null,
    @DrawableRes val icon: Int? = null,
    val children: List<Category> = emptyList()
) {
    constructor(label: String, color: Int, icon: Int?, children: List<Category>) :
            this(label, Color(color), icon, children)

    companion object {
        val EMPTY = Category("EMPTY", icon = R.drawable.school)
    }
}

