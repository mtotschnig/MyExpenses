package org.totschnig.myexpenses.provider

import android.content.ContentUris
import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.provider.filter.FtsCriterion
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class FtsDatabaseTest : BaseTestWithRepository() {

    private var testAccountId: Long = 0

    private val db: SupportSQLiteDatabase
        get() = (
            contentResolver.acquireContentProviderClient(
                TransactionProvider.AUTHORITY
            )!!.localContentProvider as TransactionProvider
            ).openHelperForTest.writableDatabase

    fun setup() {
        testAccountId = insertAccount("Wallet")
    }

    @Test
    fun testUpgradeTo192PopulatesFtsFromExistingData() {
        setup()
        val parentCatId = writeCategory("Shopping")
        val childCatId = writeCategory("Clothes", parentId = parentCatId)
        val payeeId = contentResolver.requireParty("Target")

        val tx = repository.insertTransaction(
            testAccountId,
            -8900,
            categoryId = childCatId,
            payeeId = payeeId,
            comment = "Winter jacket",
            referenceNumber = "REF-99"
        )

        val database = db

        // Clear transactions_fts table to simulate pre-migration state
        database.execSQL("DELETE FROM $TABLE_TRANSACTIONS_FTS")

        // Execute upgradeTo192() migration
        with(TransactionDatabase(application, prefHandler)) {
            database.upgradeTo192()
        }

        // Verify transactions_fts was populated correctly
        database.query(
            "SELECT rowid, comment, payee, category, ref FROM $TABLE_TRANSACTIONS_FTS WHERE rowid = ?",
            arrayOf(tx.id.toString())
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getLong(cursor.getColumnIndexOrThrow("rowid"))).isEqualTo(tx.id)
            assertThat(
                cursor.getString(cursor.getColumnIndexOrThrow("comment"))
            ).isEqualTo("Winter jacket")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("payee"))).isEqualTo("Target")
            assertThat(
                cursor.getString(cursor.getColumnIndexOrThrow("category"))
            ).isEqualTo("Clothes Shopping")
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow("ref"))).isEqualTo("REF-99")
        }
    }

    @Test
    fun testFtsQueryMatchesComment() {
        setup()
        val txId =
            repository.insertTransaction(testAccountId, -500, comment = "Grocery shopping").id
        repository.insertTransaction(testAccountId, -200, comment = "Movie tickets")

        val criterion = FtsCriterion("Grocery")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId)
        }
    }

    @Test
    fun testFtsQueryMatchesPayee() {
        setup()
        val payeeId = contentResolver.requireParty("Starbucks")
        val txId = repository.insertTransaction(testAccountId, -450, payeeId = payeeId).id

        val criterion = FtsCriterion("Starbucks")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId)
        }
    }

    @Test
    fun testFtsQueryMatchesCategoryPath() {
        setup()
        val parentCatId = writeCategory("Food & Dining")
        val childCatId = writeCategory("Groceries", parentId = parentCatId)
        val txId = repository.insertTransaction(testAccountId, -1200, categoryId = childCatId).id

        // Searching for parent category label "Food" matches subcategory transaction
        val criterion = FtsCriterion("Food")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId)
        }
    }

    @Test
    fun testFtsQueryUpdatesWhenCategoryRenamed() {
        setup()
        val parentCatId = writeCategory("Food & Dining")
        val childCatId = writeCategory("Groceries", parentId = parentCatId)
        val txId = repository.insertTransaction(testAccountId, -1200, categoryId = childCatId).id

        // Update parent category label
        contentResolver.update(
            ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, parentCatId),
            ContentValues().apply { put(KEY_LABEL, "Supermarket & Eats") },
            null,
            null
        )

        // Searching for renamed parent category label "Supermarket" matches subcategory transaction
        val criterion = FtsCriterion("Supermarket")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId)
        }
    }

    @Test
    fun testFtsQueryNonAsciiUmlauts() {
        setup()
        val txId = repository.insertTransaction(
            testAccountId,
            -3500,
            comment = "Ausflug in Österreich"
        )
            .id

        val criterion = FtsCriterion("Österreich")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId)
        }
    }

    @Test
    fun testFtsQueryStandaloneNegation() {
        setup()
        repository.insertTransaction(testAccountId, -300, comment = "coffee")
        val txId2 = repository.insertTransaction(testAccountId, -400, comment = "tea").id

        val criterion = FtsCriterion("-coffee")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId2)
        }
    }

    @Test
    fun testFtsQueryColumnDirectives() {
        setup()
        val payeeId = contentResolver.requireParty("Walmart")
        val txId = repository.insertTransaction(
            testAccountId,
            -1500,
            payeeId = payeeId,
            comment = "tea"
        ).id

        val criterion = FtsCriterion("payee:walmart comment:tea")
        contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI,
            arrayOf(KEY_ROWID),
            criterion.getSelectionForParents("transactions_extended"),
            criterion.getSelectionArgs(false),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(KEY_ROWID, txId)
        }
    }
}
