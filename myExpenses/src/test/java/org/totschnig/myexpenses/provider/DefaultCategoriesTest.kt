package org.totschnig.myexpenses.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_TRANSFER

@RunWith(RobolectricTestRunner::class)
class DefaultCategoriesTest : BaseTestWithRepository() {

    @Suppress("UNCHECKED_CAST")
    private fun setUp() = contentResolver.call(
        TransactionProvider.DUAL_URI, TransactionProvider.METHOD_SETUP_CATEGORIES, null, null
    )!!.getSerializable(TransactionProvider.KEY_RESULT) as Pair<Int, Int>

    /** Labels of the main categories with their type, without the built-in one for transfers */
    private fun mainCategories() = contentResolver.query(
        TransactionProvider.CATEGORIES_URI, arrayOf(KEY_LABEL, KEY_TYPE),
        "$KEY_PARENTID IS NULL AND $KEY_TYPE != $FLAG_TRANSFER", null, null
    )!!.use { cursor ->
        buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getInt(1).toByte()) }
    }

    private fun subCategories(parentId: Long) = contentResolver.query(
        TransactionProvider.CATEGORIES_URI, arrayOf(KEY_LABEL), "$KEY_PARENTID = ?", arrayOf(parentId.toString()), null
    )!!.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    @Test
    @Config(qualifiers = "en")
    fun setsUpEnglishTree() {
        setUp()
        val main = mainCategories()
        assertThat(main).hasSize(22)
        assertThat(main["Food"]).isEqualTo(FLAG_EXPENSE)
        assertThat(main["Salary"]).isEqualTo(FLAG_INCOME)
    }

    @Test
    @Config(qualifiers = "de")
    fun setsUpGermanTree() {
        val (inserted, _) = setUp()
        val main = mainCategories()
        assertThat(main).hasSize(31)
        assertThat(inserted).isEqualTo(31 + 55 + 181)
        assertThat(main["Wohnen"]).isEqualTo(FLAG_EXPENSE)
        assertThat(main["Sparen & Vorsorge"]).isEqualTo(FLAG_EXPENSE)
        assertThat(main["Gehalt & Arbeit"]).isEqualTo(FLAG_INCOME)
        assertThat(main).doesNotContainKey("Umbuchungen")
        // Setting up again adds nothing
        assertThat(setUp().first).isEqualTo(0)
    }

    /** The former German tree created "Lohn" with its German uuids, the new one finds it by them */
    @Test
    @Config(qualifiers = "de")
    fun extendsFormerGermanCategories() {
        val lohn = writeCategory("Lohn", uuid = "6ce93bae-319c-4ca7-bb50-341dc49cc1ab", type = FLAG_INCOME)
        writeCategory("Nettogehalt", lohn, uuid = "1bd555da-a087-4ffb-9692-82225f9da91b")
        setUp()
        val main = mainCategories()
        assertThat(main).containsKey("Lohn")
        assertThat(main).doesNotContainKey("Gehalt & Arbeit")
        assertThat(subCategories(lohn)).containsAtLeast("Nettogehalt", "Trinkgeld", "Abfindung")
        assertThat(subCategories(lohn)).doesNotContain("Gehalt")
    }
}
