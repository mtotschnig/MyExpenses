package org.totschnig.myexpenses.ui.experiment

import android.content.ClipData
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
data object ColumnFeed : Position()

@Parcelize
sealed class Field : Position() {
    private fun asList() = when (this) {
        is CombinedField -> fields
        is SimpleField -> listOf(this)
    }

    operator fun plus(other: Field): Field = CombinedField(this.asList() + other.asList())
}

@Parcelize
data class CombinedField(val fields: List<SimpleField>) : Field()

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
            if (it == field1) field1 + field2 else it
        }
        delay(100)
        positions.remove(field2)
    }

    suspend fun move(field: Field, dropPosition: Int) {
        Log.d("draggabble", "dropping $field to position $dropPosition")
        Log.d("draggabble", "old list $positions")
        val index = positions.indexOf(field)
        if (index == dropPosition || index + 1 == dropPosition) return
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
            positions[it] is ColumnFeed && (it == 0 || it == lastColumnFeed || positions[it + 1] is ColumnFeed)
        }?.let {
            positions.removeAt(it)
        }
    }

    fun addField(field: SimpleField) {
        positions.add(field)
    }

    fun removeField(field: SimpleField) {
        if (!positions.remove(field)) {
            val (index, item) = positions.withIndex().first {
                (it.value as? CombinedField)?.fields?.contains(field) == true
            }
            positions[index] = (item as CombinedField).fields.filter { it != field }.let {
                if (it.size == 1) it.first() else CombinedField(it)
            }
        }
    }

    fun allowDrop(field: Field, dropPosition: Int): Boolean {
        val index = positions.indexOf(field)
        val result = dropPosition < index || dropPosition > index + 1
        Log.d("draggabble", "allowDrop $field ($index) to position $dropPosition: $result")
        return result
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

            columns.forEach { list ->

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

                    fun allowDrop(dropPosition: Int): (Field) -> Boolean {
                        return {
                            viewModel.allowDrop(it, dropPosition)
                        }
                    }

                    val scope = rememberCoroutineScope()
                    Column(modifier = Modifier.padding(4.dp)) {
                        dropPosition++
                        key(dropPosition) {
                            DropTarget(onDropped(scope, dropPosition), allowDrop(dropPosition))
                        }

                        list.forEach { field ->
                            Log.d("draggabble", "position $dropPosition")
                            key(
                                field,
                                dropPosition
                            ) { //without key, dragAndDropSource caches the draggable node
                                DraggableItem(field, onDropped = {
                                    scope.launch {
                                        viewModel.combine(field, it)
                                    }
                                }, onLongPress = {
                                    scope.launch {
                                        viewModel.split(field as CombinedField)
                                    }
                                })
                                dropPosition++
                                DropTarget(
                                    onDropped(scope, dropPosition),
                                    allowDrop(dropPosition)
                                )
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
                        if (checked) {
                            viewModel.addField(field)
                        } else {
                            viewModel.removeField(field)
                        }
                    })
                }

            }
        }
    }

}

@Composable
fun DropTarget(onDropped: (Field) -> Unit, allowDrop: (Field) -> Boolean = { true }) {

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
                bgColor = Color.Transparent
                (event.toAndroidDragEvent().localState as? Field)?.let {
                    onDropped(it)
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
                    (it.toAndroidDragEvent().localState as? Field)?.let(allowDrop) == true
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

    var bgColor by remember { mutableStateOf(Color.White) }

    val target = remember {
        object : DragAndDropTarget {

            override fun onEntered(event: DragAndDropEvent) {
                (event.toAndroidDragEvent().localState as? Field)?.let {
                    bgColor = Color.DarkGray.copy(alpha = 0.2f)
                }
            }

            override fun onExited(event: DragAndDropEvent) {
                bgColor = Color.White
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                bgColor = Color.White
                (event.toAndroidDragEvent().localState as? Field)?.let {
                    onDropped(it)
                }
                return true
            }
        }
    }
    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(4.dp))
            .dragAndDropSource { ->
                detectTapGestures(
                    onLongPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText(field.toString(), ""),
                                localState = field
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
                    (it.toAndroidDragEvent().localState as? Field) != field
                },
                target = target
            )
            .fillMaxWidth()
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
 * splits the list on ColumnFeed
 */
fun splitList(list: List<Position>): List<List<Field>> {
    var index = 0
    var innerList = mutableListOf<Field>()
    return buildList {
        while (index < list.size) {
            when (val item = list[index]) {
                is ColumnFeed -> {
                    add(innerList)
                    innerList = mutableListOf()
                }

                is Field -> {
                    innerList.add(item)
                }
            }
            index++
        }
        add(innerList)
    }
}