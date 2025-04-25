package org.totschnig.myexpenses.activity

import android.content.ClipData
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.viewmodel.CombinedField
import org.totschnig.myexpenses.viewmodel.Field
import org.totschnig.myexpenses.viewmodel.PrintLayoutConfigurationViewModel
import org.totschnig.myexpenses.viewmodel.SimpleField

class PrintLayoutConfiguration : ProtectedFragmentActivity() {
    val viewModel: PrintLayoutConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //injector.inject(viewModel)
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.composeView.setContent {
            AppTheme {
                DraggableTableRow(viewModel)
            }

        }
    }
}

@Composable
fun DraggableTableRow(viewModel: PrintLayoutConfigurationViewModel) {
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
    BoxWithConstraints {
        Column {
            val columns = viewModel.columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .height(IntrinsicSize.Min)
                    .weight(1f)
                    .padding(16.dp)
            ) {
                var dropPosition = -1

                columns.forEachIndexed { index, list ->

                    Box(
                        contentAlignment = Alignment.TopStart,
                        modifier = Modifier
                            .animateContentSize()
                            .weight(viewModel.columnWidths[index])
                            .border(1.dp, Color.Gray)
                            .background(Color(0xFFEEEEEE)),
                    ) {


                        val scope = rememberCoroutineScope()
                        Column(
                            modifier = Modifier
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.Absolute.Left
                            ) {
                                IconButton(onClick = {
                                    viewModel.addColumn(index)
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.add_column_left),
                                        contentDescription = "Add column left"
                                    )
                                }
                                if (list.isEmpty() && columns.size > 1) {
                                    IconButton(onClick = {
                                        viewModel.removeColumn(index)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove column"
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    viewModel.addColumn(index + 1)
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.add_column_right),
                                        contentDescription = "Add column left"
                                    )
                                }
                            }
                            dropPosition++
                            key(dropPosition) {
                                DropTarget(onDropped(scope, dropPosition), allowDrop(dropPosition))
                            }

                            list.forEach { field ->
                                key(
                                    field,
                                    dropPosition
                                ) { //without key, dragAndDropSource caches the draggable node
                                    DraggableItem(
                                        field,
                                        modifier = Modifier.fillMaxWidth(),
                                        onDropped = {
                                            scope.launch {
                                                viewModel.combine(field, it)
                                            }
                                        }
                                    )
                                    dropPosition++
                                    DropTarget(
                                        onDropped(scope, dropPosition),
                                        allowDrop(dropPosition)
                                    )
                                }
                            }
                        }
                    }
                    if (index < columns.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        val totalWidth = viewModel.columnWidths.sum()
                                        val delta =
                                            (dragAmount.toDp() / this@BoxWithConstraints.maxWidth) * totalWidth
                                        val newWidthLeft = viewModel.columnWidths[index] + delta
                                        val newWidthRight =
                                            viewModel.columnWidths[index + 1] - delta
                                        val minWidth = totalWidth / 20
                                        if (newWidthLeft > minWidth && newWidthRight > minWidth) {
                                            viewModel.columnWidths[index] = newWidthLeft
                                            viewModel.columnWidths[index + 1] = newWidthRight
                                        }
                                    }
                                }
                                .background(Color.DarkGray)
                        )
                    }
                }
            }
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
                            viewModel.removeField(it)
                        }
                        return true
                    }
                }
            }
            val inactiveFields = viewModel.inactiveFields
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            (event.toAndroidDragEvent().localState as? Field)?.let {
                                inactiveFields.contains(it)
                            } == false
                        },
                        target = target
                    )
            ) {
                Column {
                    Text("Fields not printed")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        inactiveFields.forEach { field ->
                            key(field) {
                                DraggableItem(field)
                            }
                        }
                    }
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
fun DraggableItem(
    field: Field,
    modifier: Modifier = Modifier,
    onDropped: ((Field) -> Unit)? = null
) {

    var bgColor by remember { mutableStateOf(Color.White) }

    val target = remember {
        object : DragAndDropTarget {

            override fun onEntered(event: DragAndDropEvent) {
                bgColor = Color.DarkGray.copy(alpha = 0.2f)
            }

            override fun onExited(event: DragAndDropEvent) {
                bgColor = Color.White
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                bgColor = Color.White
                (event.toAndroidDragEvent().localState as? Field)?.let {
                    onDropped?.invoke(it)
                }
                return true
            }
        }
    }
    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(4.dp))
            .dragAndDropSource { ->
                detectTapGestures(
                    onPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText(field.toString(), ""),
                                localState = field
                            )
                        )
                    }
                )
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    onDropped != null &&
                            (it.toAndroidDragEvent().localState as? Field) != field
                },
                target = target
            )
            .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .padding(4.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            text = when (field) {
                is CombinedField -> field.fields.map { stringResource(it.label) }.joinToString()
                is SimpleField -> stringResource(field.label)
            },
            fontWeight = FontWeight.Bold
        )
    }
}
