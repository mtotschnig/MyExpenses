package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.util.fastForEachReversed
import androidx.lifecycle.AndroidViewModel
import com.livefront.sealedenum.GenSealedEnum
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.printLayout
import org.totschnig.myexpenses.preference.printLayoutColumnWidths
import org.totschnig.myexpenses.preference.printLayoutDefault
import org.totschnig.myexpenses.preference.printLayoutDefaultColumnsWidths
import org.totschnig.myexpenses.util.indexOfFrom
import org.totschnig.myexpenses.util.lastIndexOfFrom
import org.totschnig.myexpenses.util.removeNthOccurrence
import javax.inject.Inject

@Parcelize
@Serializable
sealed class Position : Parcelable

@Parcelize
@Serializable
data object ColumnFeed : Position()

@Parcelize
@Serializable
sealed class Field : Position() {
    fun asList(): List<SimpleField> = when (this) {
        is CombinedField -> fields
        is SimpleField -> listOf(this)
    }

    operator fun plus(other: Field): Field = CombinedField(this.asList() + other.asList())

    abstract fun toString(context: Context, extra: Bundle? = null): String
}

@Parcelize
@Serializable
data class CombinedField(val fields: List<SimpleField>) : Field() {
    override fun toString(context: Context, extra: Bundle?) =
        fields.joinToString(" / ") { it.toString(context, extra) }
}

@Serializable
sealed class SimpleField(@StringRes val label: Int) : Field() {
    @GenSealedEnum
    companion object;

    override fun toString(context: Context, extra: Bundle?) = context.getString(label)
}

@Parcelize
@Serializable
data object Date : SimpleField(R.string.date) {
    const val KEY_IS_TIME_FIELD = "isTimeField"
    override fun toString(context: Context, extra: Bundle?): String {
        val isTimeField = extra?.getBoolean(KEY_IS_TIME_FIELD) == true
        return if (isTimeField) context.getString(R.string.time) else super.toString(context, extra)
    }
}

@Parcelize
@Serializable
data object Category : SimpleField(R.string.category)

@Parcelize
@Serializable
data object Tags : SimpleField(R.string.tags)

@Parcelize
@Serializable
data object Payee : SimpleField(R.string.payee)

@Parcelize
@Serializable
data object Notes : SimpleField(R.string.notes)

@Parcelize
@Serializable
data object Amount : SimpleField(R.string.amount)

@Parcelize
@Serializable
data object ReferenceNumber : SimpleField(R.string.reference_number)

@Parcelize
@Serializable
data object Account : SimpleField(R.string.account)

@Parcelize
@Serializable
data object OriginalAmount : SimpleField(R.string.menu_original_amount)

const val MIN_WIDTH = 0.05f

class PrintLayoutConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun List<Position>.asColumns(): List<List<Field>> {
            var index = 0
            var innerList = mutableListOf<Field>()
            return buildList {
                while (index < this@asColumns.size) {
                    when (val item = this@asColumns[index]) {
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
    }

    @Inject
    lateinit var prefHandler: PrefHandler

    val positions: SnapshotStateList<Position> = SnapshotStateList()
    val columnWidths: SnapshotStateList<Float> = SnapshotStateList()

    val inactiveFields: List<SimpleField>
        get() = SimpleField.values -
                positions.filterIsInstance<Field>().flatMap { it.asList() }.toSet()

    val columns: List<List<Field>>
        get() = positions.asColumns()

    fun init() {
        positions.addAll(prefHandler.printLayout)
        val elements = prefHandler.printLayoutColumnWidths.map { it.toFloat() }
        if (elements.size == columns.size) {
            columnWidths.addAll(elements)
        } else {
            //reset to default if columnWidths is not the same size as columns
            columnWidths.addAll(List(columns.size) { 250f })
        }
    }

    suspend fun combine(field1: Field, field2: Field) {
        if (field1 == field2) return
        positions.replaceAll {
            if (it == field1) field1 + field2 else it
        }
        delay(100)
        positions.remove(field2)
    }

    suspend fun move(field: Field, dropPosition: Int) {
        val index = positions.indexOf(field)
        if (index > -1) {
            if (index == dropPosition || index + 1 == dropPosition) return
            positions.remove(field)
            delay(100)
        }
        positions.add(
            if (index == -1 || index > dropPosition) dropPosition else dropPosition - 1,
            field
        )
    }

    fun moveUp(field: Field) {
        val index = positions.indexOf(field)
        if (index > 0) {
            positions.removeAt(index)
            positions.add(index - 1, field)
        }
    }

    fun moveDown(field: Field) {
        val index = positions.indexOf(field)
        if (index < positions.lastIndex) {
            positions.removeAt(index)
            positions.add(index + 1, field)
        }
    }

    fun moveRight(field: Field) {
        val index = positions.indexOf(field)
        val newPosition = positions.indexOfFrom(ColumnFeed, index)
        if (newPosition > index) {
            positions.removeAt(index)
            positions.add(newPosition, field)
        }
    }

    fun moveLeft(field: Field) {
        val index = positions.indexOf(field)
        val newPosition = positions.lastIndexOfFrom(ColumnFeed, index)
        if (newPosition < index && newPosition != -1) {
            positions.removeAt(index)
            positions.add(newPosition, field)
        }
    }

    fun addColumn(position: Int) {
        var columnNr = 0
        val avgColumnWidth = columnWidths.average().toFloat()
        for (index in positions.indices) {
            if (columnNr == position) {
                positions.add(index, ColumnFeed)
                columnWidths.add(columnNr, avgColumnWidth)
                return
            }
            if (positions[index] is ColumnFeed) {
                columnNr++
            }
        }
        positions.add(ColumnFeed)
        columnWidths.add(avgColumnWidth)
    }

    /**
     * removes the column at the given position. Ideally this column is empty.
     * If it is not empty, two columns will be merged.
     */
    fun removeColumn(position: Int) {
        Snapshot.withMutableSnapshot {
            if (positions.isNotEmpty()) {
                positions.removeNthOccurrence(ColumnFeed, (position - 1).coerceAtLeast(0))
                columnWidths.removeAt(position)
            }
        }
    }

    fun removeField(field: Field) {
        positions.remove(field)
    }

    fun addFieldStart(field: Field) {
        positions.add(0, field)
    }

    fun addFieldEnd(field: Field) {
        positions.add(field)
    }

    fun resetToDefault() {
        Snapshot.withMutableSnapshot {
            positions.clear()
            columnWidths.clear()
            positions.addAll(printLayoutDefault)
            columnWidths.addAll(printLayoutDefaultColumnsWidths.map { it.toFloat() })
        }
    }

    fun allowDrop(field: Field, dropPosition: Int): Boolean {
        val index = positions.indexOf(field)
        return index == -1 || dropPosition < index || dropPosition > index + 1
    }

    /**
     * Triple of min value, current value and max value
     */
    fun resizeColumnInfo(column: Int): Triple<Float, Float, Float>? {
        if (columnWidths.size <= column + 1) return null
        val totalWidth = columnWidths.sum()
        val currentWidth = columnWidths[column] / totalWidth
        val currentWidthNext = columnWidths[column + 1] / totalWidth
        return Triple(
            MIN_WIDTH,
            currentWidth,
            currentWidth + (currentWidthNext - MIN_WIDTH).coerceAtLeast(0f)
        )
    }

    fun resizeColumnByPercent(columnNumber: Int, percent: Float): Boolean {
        val totalWidth = columnWidths.sum()
        val newWidthLeft = percent * totalWidth
        val delta = columnWidths[columnNumber] - newWidthLeft
        val newWidthRight = columnWidths[columnNumber + 1] + delta
        columnWidths[columnNumber] = newWidthLeft
        columnWidths[columnNumber + 1] = newWidthRight
        val minWidth = totalWidth * MIN_WIDTH
        return if (newWidthLeft > minWidth && newWidthRight > minWidth) {
            columnWidths[columnNumber] = newWidthLeft
            columnWidths[columnNumber + 1] = newWidthRight
            true
        } else false
    }

    fun resizeColumnByDelta(columnNumber: Int, deltaPercent: Float): Boolean {
        val totalWidth = columnWidths.sum()
        val delta = deltaPercent * totalWidth
        val newWidthLeft = columnWidths[columnNumber] + delta
        val newWidthRight = columnWidths[columnNumber + 1] - delta
        val minWidth = totalWidth * MIN_WIDTH
        return if (newWidthLeft > minWidth && newWidthRight > minWidth) {
            columnWidths[columnNumber] = newWidthLeft
            columnWidths[columnNumber + 1] = newWidthRight
            true
        } else false
    }


    fun save(): Boolean {
        columns.mapIndexedNotNull { index, fields ->
            if (fields.isEmpty()) index else null
        }.fastForEachReversed {
            removeColumn(it)
        }
        return if (positions.isEmpty()) false else {
            prefHandler.printLayout = positions.toList()
            prefHandler.printLayoutColumnWidths = columnWidths.map { it.toInt() }
            true
        }
    }
}