package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.MatrixCursor
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.compose.Category
import org.totschnig.myexpenses.provider.DatabaseConstants.*

@RunWith(RobolectricTestRunner::class)
class CategoryViewModelTest {

    @Test
    fun buildCatTree() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val cursor =
            MatrixCursor(arrayOf(KEY_ROWID, KEY_PARENTID, KEY_LABEL, KEY_COLOR, KEY_ICON), 8)

        fun MatrixCursor.addCat(id: Int, parent: Int?, label: String) =
            addRow(arrayOf(id, parent, label, null, null))
        cursor.addCat(1, null, "Car")
        cursor.addCat(2, 1, "Fuel")
        cursor.addCat(3, null, "Care")
        cursor.addCat(4, 3, "Clothing")
        cursor.addCat(5, 4, "Underwear")
        cursor.addCat(6, 3, "Hairdresser")
        cursor.addCat(7, null, "Food")
        cursor.addCat(8, null, "Leisure")
        cursor.moveToFirst()
        assertThat(CategoryViewModel.ingest(context, cursor, null)).containsExactly(
            Category(
                "Car", listOf(
                    Category("Fuel")
                )
            ),
            Category(
                "Care", listOf(
                    Category(
                        "Clothing", listOf(
                            Category("Underwear")
                        )
                    ),
                    Category("Hairdresser")
                )
            ),
            Category("Food"),
            Category("Leisure"),
        ).inOrder()
    }
}