package org.totschnig.shared_test

import android.database.Cursor
import com.google.common.truth.FailureMetadata
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

class CursorSubject private constructor(
    failureMetadata: FailureMetadata,
    val actual: Cursor
) : Subject(failureMetadata, actual) {
    fun hasCount(expected: Int) {
        count().isEqualTo(expected)
    }

    fun hasColumnCount(expected: Int) {
        columnCount().isEqualTo(expected)
    }

    fun hasColumns(vararg columns: String) {
        check("columns").that(actual.columnNames)
            .asList()
            .containsExactly(*columns)
            .inOrder()
    }

    fun movesToFirst() {
        check("moveToFirst").that(actual.moveToFirst()).isTrue()
    }

    fun hasString(columnName: String, expected: String) {
        check("hasString").that(actual.getString(actual.getColumnIndexOrThrow(columnName)))
            .isEqualTo(expected)
    }

    fun hasString(columnIndex: Int, expected: String) {
        check("hasString").that(actual.getString(columnIndex)).isEqualTo(expected)
    }

    fun hasLong(columnName: String, expected: Long) {
        check("hasLong").that(actual.getLong(actual.getColumnIndexOrThrow(columnName)))
            .isEqualTo(expected)
    }

    fun hasLong(columnIndex: Int, expected: Long) {
        check("hasLong").that(actual.getLong(columnIndex)).isEqualTo(expected)
    }

    fun hasInt(columnIndex: Int, expected: Int) {
        check("hasInt").that(actual.getInt(columnIndex)).isEqualTo(expected)
    }

    fun hasDouble(columnIndex: Int, expected: Double) {
        check("hasDouble").that(actual.getDouble(columnIndex)).isEqualTo(expected)
    }

    fun isNull(columnIndex: Int) {
        check("isNull").that(actual.isNull(columnIndex)).isTrue()
    }

    fun isNull(columnName: String) {
        check("isNull").that(actual.isNull(actual.getColumnIndexOrThrow(columnName))).isTrue()
    }

    fun forEach(assertions: CursorSubject.() -> Unit) {
        actual.moveToFirst()
        while(!actual.isAfterLast) {
            @Suppress("UNUSED_EXPRESSION")
            assertions()
            actual.moveToNext()
        }
    }

    private fun count(): IntegerSubject = check("count").that(actual.count)

    private fun columnCount(): IntegerSubject = check("columnCount").that(actual.columnCount)

    companion object {
        inline fun <R> Cursor?.useAndAssert(assertions: CursorSubject.() -> R) =
            this!!.use { assertThat(it).assertions() }

        fun assertThat(cursor: Cursor): CursorSubject = assertAbout(CURSOR_FACTORY).that(cursor)

        private val CURSOR_FACTORY: Factory<CursorSubject, Cursor> = Factory {
            failureMetadata: FailureMetadata, subject: Cursor? -> CursorSubject(failureMetadata, subject!!)
        }
    }
}