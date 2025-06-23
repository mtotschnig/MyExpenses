package org.totschnig.myexpenses.activity

import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.LAYOUT_DIRECTION_LTR
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.databinding.ActivityComposeBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.CombinedField
import org.totschnig.myexpenses.viewmodel.Field
import org.totschnig.myexpenses.viewmodel.PrintLayoutConfigurationViewModel
import org.totschnig.myexpenses.viewmodel.SimpleField
import timber.log.Timber

class PrintLayoutConfiguration : EditActivity() {
    val viewModel: PrintLayoutConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        if (savedInstanceState == null) {
            viewModel.init()
        }
        val binding = ActivityComposeBinding.inflate(layoutInflater)
        floatingActionButton = binding.fab.CREATECOMMAND.also {
            it.isVisible = true
        }
        setContentView(binding.root)
        setupToolbar()
        title = screenTitle
        binding.composeView.setContent {
            AppTheme {
                DraggableTableRow()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(Menu.NONE, R.id.DEFAULT_COMMAND, Menu.NONE, R.string.menu_restore)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == R.id.DEFAULT_COMMAND) {
            viewModel.resetToDefault()
            true
        } else super.onOptionsItemSelected(item)

    override val fabDescription: Int
        get() = R.string.menu_save

    override val fabActionName: String
        get() = "PRINT_CONFIGURATION"

    override fun saveState() {
        super.saveState()
        if (viewModel.save()) {
            finish()
        } else {
            isSaving = false
            showSnackBar(getString(R.string.print_configuration_empty))
        }
    }

    @Composable
    fun DraggableTableRow() {
        fun onDropped(scope: CoroutineScope, dropPosition: Int): (Field) -> Unit {
            return {
                setDirty()
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

        val nestedScrollInterop = rememberNestedScrollInteropConnection()
        BoxWithConstraints(
            modifier = Modifier
                .nestedScroll(nestedScrollInterop)
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_main_screen)
                )
        ) {
            Column {
                val columns = viewModel.columns
                Row(
                    modifier = Modifier
                        .semantics {
                            collectionInfo = CollectionInfo(
                                rowCount = columns.maxOf { it.size },
                                columnCount = columns.size
                            )
                        }
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .height(IntrinsicSize.Min)
                        .weight(1f, fill = false)
                ) {
                    var dropPosition = -1

                    columns.forEachIndexed { columnNumber, list ->

                        Box(
                            contentAlignment = Alignment.TopStart,
                            modifier = Modifier
                                .animateContentSize()
                                .weight(viewModel.columnWidths.getOrNull(columnNumber) ?: 1f)
                                .border(1.dp, Color.Gray)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                        ) {


                            val scope = rememberCoroutineScope()
                            Column(Modifier.semantics { isTraversalGroup = true }) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    if (list.isNotEmpty()) {
                                        IconButton(onClick = {
                                            setDirty()
                                            viewModel.addColumn(columnNumber)
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.add_column_start),
                                                contentDescription = "Add column left"
                                            )
                                        }
                                    }
                                    if (list.isEmpty() && columns.size > 1) {
                                        IconButton(onClick = {
                                            setDirty()
                                            viewModel.removeColumn(columnNumber)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove column"
                                            )
                                        }
                                    }
                                    if (list.isNotEmpty()) {
                                        IconButton(onClick = {
                                            setDirty()
                                            viewModel.addColumn(columnNumber + 1)
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.add_column_end),
                                                contentDescription = "Add column right"
                                            )
                                        }
                                    }
                                }
                                dropPosition++
                                key(dropPosition) {
                                    DropTarget(
                                        onDropped(scope, dropPosition),
                                        allowDrop(dropPosition)
                                    )
                                }

                                list.forEachIndexed { rowNumber, field ->
                                    key(
                                        field,
                                        dropPosition
                                    ) { //without key, dragAndDropSource caches the draggable node
                                        DraggableItem(
                                            field,
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .fillMaxWidth()
                                                .semantics {
                                                    collectionItemInfo = CollectionItemInfo(
                                                        columnIndex = columnNumber,
                                                        rowIndex = rowNumber,
                                                        columnSpan = 1,
                                                        rowSpan = 1
                                                    )
                                                    customActions = buildList {
                                                        if (rowNumber < list.lastIndex) {
                                                            add(
                                                                CustomAccessibilityAction(
                                                                    label = "moveDownLabel",
                                                                    action = {
                                                                        viewModel.moveDown(field)
                                                                        true
                                                                    }
                                                                )
                                                            )
                                                        }
                                                        if (rowNumber > 0) {
                                                            add(
                                                                CustomAccessibilityAction(
                                                                    label = "moveUpLabel",
                                                                    action = {
                                                                        viewModel.moveUp(field)
                                                                        true
                                                                    }
                                                                )
                                                            )
                                                        }
                                                        if (columnNumber < columns.lastIndex) {
                                                            add(
                                                                CustomAccessibilityAction(
                                                                    label = "moveRightLabel",
                                                                    action = {
                                                                        viewModel.moveRight(field)
                                                                        true
                                                                    }
                                                                )
                                                            )
                                                        }
                                                        if (columnNumber > 0) {
                                                            add(
                                                                CustomAccessibilityAction(
                                                                    label = "moveLeftLabel",
                                                                    action = {
                                                                        viewModel.moveLeft(field)
                                                                        true
                                                                    }
                                                                ))
                                                        }
                                                        add(
                                                            CustomAccessibilityAction(
                                                                label = "remove",
                                                                action = {
                                                                    viewModel.removeField(field)
                                                                    true
                                                                }
                                                            )
                                                        )
                                                    }
                                                },
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
                        val layoutDirection =
                            LocalConfiguration.current.layoutDirection
                        if (columnNumber < columns.lastIndex) {
                            val resizeColumnInfo = viewModel.resizeColumnInfo(columnNumber)
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(48.dp)
                                    .optional(resizeColumnInfo) {
                                        semantics {
                                            val (min, current, max) = it
                                            contentDescription = "Resize column"
                                            stateDescription =
                                                "Column ${columnNumber + 1} : ${(100 * current).toInt()} %"
                                            progressBarRangeInfo = ProgressBarRangeInfo(
                                                current = current,
                                                range = min..max,
                                                steps = ((max - min) * 100).toInt()
                                            )
                                            setProgress {
                                                if (viewModel.resizeColumnByPercent(
                                                        columnNumber,
                                                        it
                                                    )
                                                ) {
                                                    setDirty()
                                                    true
                                                } else false
                                            }
                                        }
                                    }
                                    .pointerInput(Unit) {

                                        detectHorizontalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val dragAmountDp =
                                                (if (layoutDirection == LAYOUT_DIRECTION_LTR)
                                                    dragAmount else -dragAmount).toDp()
                                            if (viewModel.resizeColumnByDelta(
                                                    columnNumber,
                                                    dragAmountDp / this@BoxWithConstraints.maxWidth
                                                )
                                            ) {
                                                setDirty()
                                            }
                                        }
                                    }
                                    .onKeyEvent {
                                        when (it.key) {
                                            Key.DirectionLeft -> {
                                                viewModel.resizeColumnByDelta(columnNumber, -0.01f)
                                                true
                                            }

                                            Key.DirectionRight -> {
                                                viewModel.resizeColumnByDelta(columnNumber, 0.01f)
                                                true
                                            }

                                            else -> false
                                        }
                                    }
                                    .background(MaterialTheme.colorScheme.inverseSurface)
                            )
                        }
                    }
                }
                var dragStarted by remember { mutableStateOf(false) }
                val targeted = MaterialTheme.colorScheme.surfaceContainerHighest
                var bgColor by remember { mutableStateOf(Color.Transparent) }
                val target = remember {
                    object : DragAndDropTarget {
                        override fun onStarted(event: DragAndDropEvent) {
                            dragStarted = true
                        }

                        override fun onEntered(event: DragAndDropEvent) {
                            bgColor = targeted
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

                        override fun onEnded(event: DragAndDropEvent) {
                            dragStarted = false
                        }
                    }
                }
                val inactiveFields = viewModel.inactiveFields
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(bgColor, RoundedCornerShape(16.dp))
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event ->
                                ((event.toAndroidDragEvent().localState as? Field)?.let {
                                    inactiveFields.contains(it)
                                } == false).also {
                                    Timber.d("shouldStartDragAndDrop: $it")
                                }
                            },
                            target = target
                        )
                ) {
                    if (dragStarted) {
                        Icon(
                            imageVector = Icons.Default.Delete, contentDescription = "Remove field",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            inactiveFields.forEach { field ->
                                key(field) {
                                    DraggableItem(field, Modifier.semantics {
                                        customActions = listOf(
                                            CustomAccessibilityAction(
                                                label = "Insert at front",
                                                action = {
                                                    viewModel.addFieldStart(field)
                                                    true
                                                }
                                            ),
                                            CustomAccessibilityAction(
                                                label = "Insert at end",
                                                action = {
                                                    viewModel.addFieldEnd(field)
                                                    true
                                                })
                                        )
                                    })
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    companion object {
        val Context.screenTitle
            get() = getString(R.string.menu_print) + ": " + getString(R.string.layout)
    }
}

@Composable
fun DropTarget(onDropped: (Field) -> Unit, allowDrop: (Field) -> Boolean = { true }) {

    val targeted = MaterialTheme.colorScheme.surface
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                bgColor = targeted
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
            .background(bgColor)
            .height(32.dp)
            .fillMaxWidth()
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    (it.toAndroidDragEvent().localState as? Field)?.let(allowDrop) == true
                },
                target = target
            ), contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.semantics {
                hideFromAccessibility()
            },
            painter = painterResource(R.drawable.step_into),
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableItem(
    field: Field,
    modifier: Modifier = Modifier,
    onDropped: ((Field) -> Unit)? = null,
) {
    val targeted = MaterialTheme.colorScheme.surface
    val normal = MaterialTheme.colorScheme.surfaceContainerHighest
    var bgColor by remember { mutableStateOf(normal) }

    val target = remember {
        object : DragAndDropTarget {

            override fun onEntered(event: DragAndDropEvent) {
                bgColor = targeted
            }

            override fun onExited(event: DragAndDropEvent) {
                bgColor = normal
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                bgColor = normal
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
