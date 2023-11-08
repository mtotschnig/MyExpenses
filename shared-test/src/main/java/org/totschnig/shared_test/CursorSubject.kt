package org.totschnig.shared_test

import android.database.Cursor
import com.google.common.truth.FailureMetadata
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

class CursorSubject private constructor(
    failureMetadata: FailureMetadata,
    private val actual: Cursor
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
        check("hasString").that(actual.getString(actual.getColumnIndexOrThrow(columnName))).isEqualTo(expected)
    }

    fun hasString(columnIndex: Int, expected: String) {
        check("hasString").that(actual.getString(columnIndex)).isEqualTo(expected)
    }

    fun hasLong(columnName: String, expected: Long) {
        check("hasLong").that(actual.getLong(actual.getColumnIndexOrThrow(columnName))).isEqualTo(expected)
    }

    fun hasLong(columnIndex: Int, expected: Long) {
        check("hasLong").that(actual.getLong(columnIndex)).isEqualTo(expected)
    }

    fun hasInt(columnIndex: Int, expected: Int) {
        check("hasLong").that(actual.getInt(columnIndex)).isEqualTo(expected)
    }

    private fun count(): IntegerSubject = check("count").that(actual.count)

    private fun columnCount(): IntegerSubject = check("columnCount").that(actual.count)

    companion object {
        fun assertThat(cursor: Cursor): CursorSubject {
            return Truth.assertAbout(CURSOR_FACTORY).that(cursor)
        }

        private val CURSOR_FACTORY = Factory { failureMetadata: FailureMetadata, subject: Cursor ->
            CursorSubject(failureMetadata, subject)
        }
    }
}