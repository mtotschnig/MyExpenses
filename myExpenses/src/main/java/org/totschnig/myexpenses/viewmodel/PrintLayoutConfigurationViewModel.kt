package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.livefront.sealedenum.GenSealedEnum
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.printLayout
import org.totschnig.myexpenses.preference.printLayoutColumnWidth
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
}

@Parcelize
@Serializable
data class CombinedField(val fields: List<SimpleField>) : Field()

@Serializable
sealed class SimpleField(@StringRes val label: Int) : Field() {
    @GenSealedEnum
    companion object
}

@Parcelize
@Serializable
data object Date : SimpleField(R.string.date)

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


class PrintLayoutConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var prefHandler: PrefHandler

    val positions: SnapshotStateList<Position> = SnapshotStateList()
    val columnWidths: SnapshotStateList<Float> = SnapshotStateList()

    val inactiveFields: List<SimpleField>
        get() = SimpleField.values -
                positions.filterIsInstance<Field>().flatMap { it.asList() }.toSet()

    val columns: List<List<Field>>
        get() {
            var index = 0
            var innerList = mutableListOf<Field>()
            return buildList {
                while (index < positions.size) {
                    when (val item = positions[index]) {
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

    fun init() {
        positions.addAll(prefHandler.printLayout)
        val elements = prefHandler.printLayoutColumnWidth
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
     * removes the positionth column. Ideally this column is empty.
     * If it is not empty, two columns will be merged.
     */
    fun removeColumn(position: Int) {
        positions.removeNthOccurrence(ColumnFeed, (position-1).coerceAtLeast(0))
        columnWidths.removeAt(position)
    }

    fun removeField(field: Field) {
        positions.remove(field)
    }

    fun allowDrop(field: Field, dropPosition: Int): Boolean {
        val index = positions.indexOf(field)
        return index == -1 || dropPosition < index || dropPosition > index + 1
    }

    fun save() {
        prefHandler.printLayout = positions.toList()
        prefHandler.printLayoutColumnWidth = columnWidths

    }
}