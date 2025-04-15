package com.example.draganddropcompose

import android.content.ClipData
import android.util.Log
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

const val COLUMNS = 5
const val ROWS = 5

enum class Field {
    A, B, C, D, E
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableTableRow() {

    val positions = remember {
        mutableStateMapOf(
            Pair(0, 1) to Field.A,
            Pair(0, 3) to Field.B,
            Pair(1, 0) to Field.C,
            Pair(1, 4) to Field.D,
            Pair(2, 2) to Field.E,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        repeat(COLUMNS) { columnNr ->

            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .weight(1f)
                    .height(250.dp)
                    .border(1.dp, Color.Gray)
                    .background(Color(0xFFEEEEEE)),
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    repeat(ROWS) { rowNr ->
                        val position = columnNr to rowNr
                        key(position) {

                            DropTarget(positions, position)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DropTarget(positions: SnapshotStateMap<Pair<Int, Int>, Field>, position: Pair<Int, Int>) {
    val field = positions[position]
    var bgColor by remember { mutableStateOf(Color.White) }
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                Log.d("draggable", "onEntered")
                bgColor = Color.DarkGray.copy(alpha = 0.2f)
            }

            override fun onExited(event: DragAndDropEvent) {
                Log.d("draggable", "onExited")
                bgColor = Color.Transparent
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null &&
                    clipData.itemCount > 0 &&
                    clipData.getItemAt(0)?.text != null
                ) {
                    val draggedLabel =
                        event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                    //TODO
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
                shouldStartDragAndDrop = { event -> event.toAndroidDragEvent().clipDescription.label.toString() != field?.name
                },
                target = target
            ), contentAlignment = Alignment.Center) {
        if (field != null) {
            DraggableItem(field.name)
        } else { Text(position.toString()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableItem(
    label: String,
) {
    Log.d("draggable", "composing $label")
    Box(
        modifier = Modifier
            .dragAndDropSource { ->
                detectTapGestures(
                    onPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText(label, label),
                                flags = View.DRAG_FLAG_GLOBAL,
                            )
                        )
                    })
            }
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(4.dp))
            .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .padding(4.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            text = label,
            fontWeight = FontWeight.Bold
        )
    }
}
