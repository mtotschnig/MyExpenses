package org.totschnig.myexpenses.viewmodel

import android.database.MatrixCursor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.provider.DatabaseConstants.*

@RunWith(RobolectricTestRunner::class)
class CategoryViewModelTest {
    @Test
    fun buildCatTree() {
        val cursor = MatrixCursor(arrayOf(KEY_ROWID, KEY_PARENTID, KEY_LABEL), 10)
        cursor.addRow(arrayOf(1, null, "Car"))
        cursor.addRow(arrayOf(2, 1, "Fuel"))
        cursor.addRow(arrayOf(3, null, "Care"))
        cursor.addRow(arrayOf(4, 3, "Clothing"))
        cursor.addRow(arrayOf(5, 4, "Underwear"))
        cursor.addRow(arrayOf(6, 3, "Hairdresser"))
        cursor.addRow(arrayOf(7, null, "Food"))
        cursor.moveToFirst()
        val categoryList = CategoryViewModel.ingest(cursor, null)
        assertThat(categoryList).hasSize(3)

        assertThat(categoryList[0].label).isEqualTo("Car")
        with(categoryList[0].children) {
            assertThat(this).hasSize(1)
            assertThat(get(0).label).isEqualTo("Fuel")
            assertThat(get(0).children).isEmpty()
        }

        assertThat(categoryList[1].label).isEqualTo("Care")
        with(categoryList[1].children) {
            assertThat(this).hasSize(2)
            assertThat(get(0).label).isEqualTo("Clothing")
            with(get(0).children) {
                assertThat(this).hasSize(1)
                assertThat(get(0).label).isEqualTo("Underwear")
                assertThat(get(0).children).isEmpty()
            }
            assertThat(get(1).label).isEqualTo("Hairdresser")
            assertThat(get(1).children).isEmpty()
        }

        assertThat(categoryList[2].label).isEqualTo("Food")
        assertThat(categoryList[2].children).isEmpty()
    }
}