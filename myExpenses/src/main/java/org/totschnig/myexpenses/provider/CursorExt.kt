package org.totschnig.myexpenses.provider

import android.database.Cursor
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
import java.time.LocalDate

fun <T> Cursor.useAndMapToOne(mapper: (Cursor) -> T) = use {
    if (it.moveToFirst()) mapper(it) else null
}

fun <T> Cursor.useAndMapToList(mapper: (Cursor) -> T) = use {
    it.asSequence.map(mapper).toList()
}

fun <T> Cursor.useAndMapToSet(mapper: (Cursor) -> T) = use {
    it.asSequence.map(mapper).toSet()
}

fun <T> Cursor.useAndReduce(initial: T, mapper: (T, Cursor) -> T): T = use {
    it.asSequence.fold(initial) { acc, cursor -> mapper(acc, cursor) }
}

fun <K, V> Cursor.useAndMapToMap(mapper: (Cursor) -> Pair<K, V>?) = use { cursor ->
    buildMap {
        cursor.asSequence.forEach {
            mapper(it)?.let { (key, value) ->
                put(key, value)
            }
        }
    }
}

/**
 * requires the Cursor to be positioned BEFORE first row
 */
val Cursor.asSequence: Sequence<Cursor>
    get() {
        check(isBeforeFirst)
        return generateSequence { takeIf { it.moveToNext() } }
    }

fun Cursor.requireString(columnIndex: Int) = getStringOrNull(columnIndex) ?: ""
fun Cursor.getString(column: String) = requireString(getColumnIndexOrThrow(column))
fun Cursor.getInt(column: String) = getInt(getColumnIndexOrThrow(column))
fun Cursor.getLong(column: String) = getLong(getColumnIndexOrThrow(column))
fun Cursor.getDouble(column: String) = getDouble(getColumnIndexOrThrow(column))
fun Cursor.getStringOrNull(column: String, allowEmpty: Boolean = false) =
    getStringOrNull(getColumnIndexOrThrow(column))?.takeIf { allowEmpty || it.isNotEmpty() }

fun Cursor.getIntOrNull(column: String) = getIntOrNull(getColumnIndexOrThrow(column))
fun Cursor.getLongOrNull(column: String) = getLongOrNull(getColumnIndexOrThrow(column))
fun Cursor.requireLong(column: String) = getLongOrNull(getColumnIndexOrThrow(column)) ?: 0L
fun Cursor.getIntIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getInt(it) }

fun Cursor.getIntIfExistsOr0(column: String) = getIntIfExists(column) ?: 0
fun Cursor.getLongIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getLongOrNull(it) }

fun Cursor.getLongIfExistsOr0(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getLong(it) } ?: 0L

fun Cursor.getStringIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getStringOrNull(it) }

fun Cursor.getDoubleIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getDouble(it) }

fun Cursor.getDoubleOrNull(column: String) = getDoubleOrNull(getColumnIndexOrThrow(column))


fun Cursor.getBoolean(column: String) = getInt(column) == 1
fun Cursor.getBoolean(columnIndex: Int) = getInt(columnIndex) == 1

fun Cursor.getBooleanIfExists(column: String): Boolean? = getIntIfExists(column)?.equals(1)

fun Cursor.isNull(column: String) = isNull(getColumnIndexOrThrow(column))

inline fun <reified T : Enum<T>> Cursor.getEnum(column: String, default: T) =
    enumValueOrDefault(getString(column), default)

inline fun <reified T : Enum<T>> Cursor.getEnum(columnIndex: Int, default: T) =
    enumValueOrDefault(getString(columnIndex), default)

inline fun <reified T : Enum<T>> Cursor.getEnumOrNull(columnIndex: Int) =
    enumValueOrNull<T>(getString(columnIndex))

/**
 * Splits the value of column by ASCII UnitSeparator char
 */
fun Cursor.splitStringList(colum: String) = getString(colum)
    .takeIf { it.isNotEmpty() }
    ?.split('')
    ?: emptyList()

fun Cursor.getLocalDate(columnIndex: Int) = LocalDate.parse(getString(columnIndex))
fun Cursor.getLocalDate(column: String) = getLocalDate(getColumnIndexOrThrow(column))
fun Cursor.getLocalDateIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getLocalDate(it) }
