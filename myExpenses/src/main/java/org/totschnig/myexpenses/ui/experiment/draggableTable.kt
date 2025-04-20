package org.totschnig.myexpenses.ui.experiment

import android.content.ClipData
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livefront.sealedenum.GenSealedEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Position : Parcelable

@Parcelize
sealed class ColumnContent : Position()

@Parcelize
data object ColumnFeed : Position()

@Parcelize
sealed class Field : ColumnContent()

@Parcelize
data class CombinedField(val fields: List<SimpleField>) : Field()

@Parcelize
data object LineFeed : ColumnContent()

sealed class SimpleField : Field() {
    @GenSealedEnum
    companion object
}

@Parcelize
data object A : SimpleField()

@Parcelize
data object B : SimpleField()

@Parcelize
data object C : SimpleField()

@Parcelize
data object D : SimpleField()

@Parcelize
data object E : SimpleField()


class MyViewModel : ViewModel() {

    val positions: SnapshotStateList<Position> = SnapshotStateList()

    init {
        viewModelScope.launch {
            positions.addAll(
                listOf(
                    CombinedField(listOf(A, B)),
                    ColumnFeed,
                    C,
                    ColumnFeed,
                    D,
                    ColumnFeed,
                    E,
                )
            )
        }
    }

    suspend fun split(combinedField: CombinedField) {
        if (combinedField.fields.size == 1) return
        Log.d("draggabble", "splitting $combinedField")
        val position = positions.indexOf(combinedField);
        positions.remove(combinedField)
        delay(100)
        combinedField.fields.forEachIndexed { index, field ->
            positions.add(position + index, field)
        }
    }

    suspend fun combine(field1: Field, field2: Field) {
        if (field1 == field2) return
        Log.d("draggabble", "combining $field1 and $field2")
        positions.replaceAll {
            if (it == field1) CombinedField(buildList {
                when (field1) {
                    is CombinedField -> addAll(field1.fields)
                    is SimpleField -> add(field1)
                }
                when (field2) {
                    is CombinedField -> addAll(field2.fields)
                    is SimpleField -> add(field2)
                }
            }) else it
        }
        delay(100)
        positions.remove(field2)

    }

    suspend fun move(field: Field, dropPosition: Int) {
        Log.d("draggabble", "dropping $field to position $dropPosition")
        Log.d("draggabble", "old list $positions")
        val index = positions.indexOf(field)
        if (index == dropPosition || index +1 == dropPosition) return
        positions.remove(field)
        delay(100)
        positions.add(if (index > dropPosition) dropPosition else dropPosition - 1, field)

        Log.d("draggabble", "new list $positions")
    }

    fun addColumn() {
        positions.add(ColumnFeed)
    }

    //remove the first empty column, or if there are no empty columns, the last column
    fun removeColumn() {
        val lastColumnFeed = positions.indexOfLast { it is ColumnFeed }
        if (lastColumnFeed == -1) return
        positions.indices.find {
            positions[it] is ColumnFeed && (it == 0 || it == lastColumnFeed || positions[it+1] is ColumnFeed)
        }?.let {
            positions.removeAt(it)
        }
    }
}

@Preview
@Composable
fun DraggableTableRow() {
    val viewModel = remember { MyViewModel() }
    Column {
        val columns = splitList(viewModel.positions)
        Row {
            if (columns.none { it.isEmpty() } && columns.any { it.size > 1 }) {
                Button(viewModel::addColumn) {
                    Text("add column")
                }
            }
            if (viewModel.positions.count { it is ColumnFeed } > 0) {
                Button(viewModel::removeColumn) {
                    Text("remove column")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            var dropPosition = -1

            columns.forEachIndexed { columnNr, list ->

                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .animateContentSize()
                        .weight(1f)
                        .border(1.dp, Color.Gray)
                        .background(Color(0xFFEEEEEE)),
                ) {
                    fun onDropped(scope: CoroutineScope, dropPosition: Int): (Field) -> Unit {
                        return {
                            scope.launch {
                                viewModel.move(it, dropPosition)
                            }
                        }
                    }

                    val scope = rememberCoroutineScope()
                    Column(modifier = Modifier.padding(4.dp)) {
                        dropPosition++
                        key(dropPosition) {
                            DropTarget(onDropped(scope, dropPosition))
                        }

                        list.forEach { field ->
                            Log.d("draggabble", "position $dropPosition")
                            key(
                                field,
                                dropPosition
                            ) { //without key, dragAndDropSource caches the draggable node
                                when (field) {
                                    is Field -> {
                                        DraggableItem(field, onDropped = {
                                            scope.launch {
                                                viewModel.combine(field, it)
                                            }
                                        }, onLongPress = {
                                            scope.launch {
                                                viewModel.split(field as CombinedField)
                                            }
                                        })
                                    }

                                    LineFeed -> TODO()
                                }
                                dropPosition++
                                DropTarget(onDropped(scope, dropPosition))
                            }
                        }
                    }
                }
            }
        }
        FlowRow {
            SimpleField.values.forEach { field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(field::class.java.simpleName)
                    Checkbox(checked = viewModel.positions.any {
                        when (it) {
                            is CombinedField -> field in it.fields
                            is SimpleField -> it == field
                            else -> false
                        }
                    }, onCheckedChange = { checked ->
                        if(checked) {
                            viewModel.positions.add(field)
                        } else {
                            if(!viewModel.positions.remove(field)) {
                                val (index, item) = viewModel.positions.withIndex().first {
                                    (it.value as? CombinedField)?.fields?.contains(field) == true
                                }
                                viewModel.positions[index] = CombinedField((item as CombinedField).fields.filter { it != field })
                            }
                        }
                    })
                }

            }
        }
    }

}

@Composable
fun DropTarget(onDropped: (Field) -> Unit) {

    var bgColor by remember { mutableStateOf(Color.Transparent) }
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                bgColor = Color.DarkGray.copy(alpha = 0.2f)
            }

            override fun onExited(event: DragAndDropEvent) {
                bgColor = Color.Transparent
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null &&
                    clipData.itemCount > 0
                ) {
                    val intent = event.toAndroidDragEvent().clipData.getItemAt(0).intent
                    intent.setExtrasClassLoader(Field::class.java.classLoader)
                    onDropped(
                        intent.getParcelableExtra<Field>(
                            "field"
                        ) as Field
                    )
                }
                return true
            }
        }
    }
    Box(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
            .background(bgColor, RoundedCornerShape(16.dp))
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    /* it.toAndroidDragEvent()
                        .clipData
                        .getItemAt(0)
                        .intent.getSerializableExtra("field") as Field !in dontAccept
*/                                        true
                },
                target = target
            ), contentAlignment = Alignment.Center
    ) {
        Text("Drop here")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableItem(field: Field, onDropped: (Field) -> Unit, onLongPress: () -> Unit) {
    @Suppress("DEPRECATION")
    val target = remember {
        object : DragAndDropTarget {

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null &&
                    clipData.itemCount > 0
                ) {
                    val intent = event.toAndroidDragEvent().clipData.getItemAt(0).intent
                    intent.setExtrasClassLoader(Field::class.java.classLoader)
                    onDropped(
                        intent.getParcelableExtra<Field>(
                            "field"
                        ) as Field
                    )
                }
                return true
            }
        }
    }
    Box(
        modifier = Modifier
            .dragAndDropSource { ->
                detectTapGestures(
                    onLongPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newIntent(
                                    field::class.java.simpleName,
                                    Intent().apply {
                                        putExtra("field", field)
                                    }
                                ),
                            )
                        )
                    }, onDoubleTap = {
                        if (field is CombinedField) {
                            onLongPress()
                        }
                    })
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    true
                },
                target = target
            )
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(4.dp))
            .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .padding(4.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            text = when (field) {
                is CombinedField -> field.fields.joinToString { it::class.java.simpleName }
                else -> field::class.java.simpleName
            },
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * splits the list on ColumnFeed, and adds an empty list at the end so
 */
fun splitList(list: List<Position>): List<List<ColumnContent>> {
    var index = 0
    var innerList = mutableListOf<ColumnContent>()
    return buildList {
        while (index < list.size) {
            when (val item = list[index]) {
                is ColumnFeed -> {
                    add(innerList)
                    innerList = mutableListOf()
                }

                is ColumnContent -> {
                    innerList.add(item)
                }
            }
            index++
        }
        add(innerList)
    }
}
