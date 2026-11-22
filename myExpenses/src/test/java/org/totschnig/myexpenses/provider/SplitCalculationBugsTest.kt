package org.totschnig.myexpenses.provider

import android.content.ContentUris
import android.content.ContentValues
import androidx.datastore.preferences.core.edit
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.preference.dynamicExchangeRatesDefaultKey
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.viewmodel.data.Debt
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class SplitCalculationBugsTest : BaseTestWithRepository() {

    private var foreignAccountId: Long = 0
    private var expenseCategory1: Long = 0
    private var expenseCategory2: Long = 0
    private var splitParentId: Long = 0
    private var part1Id: Long = 0
    private var part2Id: Long = 0
    private var payeeId: Long = 0
    private var debtId: Long = 0

    @Before
    fun setup() {
        expenseCategory1 =
            repository.saveCategory(Category(label = "Expense 1", type = FLAG_EXPENSE))!!
        expenseCategory2 =
            repository.saveCategory(Category(label = "Expense 2", type = FLAG_EXPENSE))!!

        // Insert foreign account (EUR) with dynamic exchange rates enabled
        foreignAccountId = insertAccount("EUR Account", currency = "EUR", dynamic = true)

        // Save exchange rate: 1 EUR = 1.50 USD
        val today = LocalDate.now()
        repository.savePrice("USD", "EUR", today, ExchangeRateSource.User, 1.50)

        // Create a split transaction in EUR account:
        // Parent: -100 EUR (-10000 minor units), equivalent = -15000 (-150.00 USD)
        // Part 1: -60 EUR (-6000 minor units), equivalent = -9000 (-90.00 USD)
        // Part 2: -40 EUR (-4000 minor units), equivalent = -6000 (-60.00 USD)
        val split = repository.createSplitTransaction(
            Transaction(
                accountId = foreignAccountId,
                amount = -10000,
                categoryId = SPLIT_CATID,
                equivalentAmount = -15000,
                uuid = generateUuid()
            ),
            listOf(
                Transaction(
                    accountId = foreignAccountId,
                    amount = -6000,
                    categoryId = expenseCategory1,
                    equivalentAmount = -9000,
                    uuid = generateUuid()
                ),
                Transaction(
                    accountId = foreignAccountId,
                    amount = -4000,
                    categoryId = expenseCategory2,
                    equivalentAmount = -6000,
                    uuid = generateUuid()
                )
            )
        )

        splitParentId = split.data.id
        part1Id = split.splitParts!![0].data.id
        part2Id = split.splitParts[1].data.id

        // Create a Payee and Debt for testing debt code path
        payeeId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.PAYEES_URI,
                PayeeInfo("Test Payee").contentValues
            )!!
        )
        debtId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.DEBTS_URI,
                Debt(
                    0,
                    "Test Debt",
                    "",
                    payeeId,
                    100000,
                    CurrencyUnit.DebugInstance,
                    System.currentTimeMillis() / 1000
                ).toContentValues()
            )!!
        )

        // Associate split part 1 with the debt
        contentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,
            ContentValues(1).apply { put(KEY_DEBT_ID, debtId) },
            "$KEY_ROWID = ?",
            arrayOf(part1Id.toString())
        )
    }

    /**
     * Code path 1: buildTransactionGroupCte
     * Tests transaction grouping calculation in home currency (USD).
     * Expected sum_expenses: -15000 (-150.00 USD)
     */
    @Test
    fun testTransactionGroupCteHomeCurrencyCalculation() {
        val uri = BaseTransactionProvider.groupingUriBuilder(Grouping.NONE).build()

        contentResolver.query(uri, null, null, null, null).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(actual.getColumnIndexOrThrow(KEY_SUM_EXPENSES), -15000)
        }
    }

    /**
     * Code path 2: buildSearchCte
     * Tests TRANSACTIONS_SUM_URI calculation in home currency (USD).
     * Expected sum_expenses: -15000 (-150.00 USD)
     */
    @Test
    fun testSearchCteHomeCurrencySumCalculation() {
        val uri = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL, "true")
            .build()

        contentResolver.query(uri, arrayOf(KEY_SUM_EXPENSES), null, null, null).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, -15000)
        }
    }

    /**
     * Code path 3: amountCteForDebts
     * Tests DEBTS_URI calculation in home currency (USD) for a debt linked to a split part.
     * Expected equivalent sum for part 1: -9000 (-90.00 USD)
     */
    @Test
    fun testAmountCteForDebtsHomeCurrencyCalculation() {
        val uri = TransactionProvider.DEBTS_URI.buildUpon().build()

        contentResolver.query(
            uri,
            null,
            "$TABLE_DEBTS.$KEY_ROWID = ?",
            arrayOf(debtId.toString()),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(actual.getColumnIndexOrThrow(KEY_EQUIVALENT_SUM), -9000)
        }
    }

    /**
     * Code path 4: KEY_AMOUNT_HOME_EQUIVALENT on EXTENDED_URI / TRANSACTIONS_URI
     * Tests querying KEY_AMOUNT_HOME_EQUIVALENT directly for a split part transaction.
     * Expected equivalent amount for part 1 (-60 EUR): -9000 (-90.00 USD)
     */
    @Test
    fun testAmountHomeEquivalentProjectionOnSplitPart() {
        val uri = TransactionProvider.EXTENDED_URI

        contentResolver.query(
            uri,
            arrayOf(KEY_AMOUNT_HOME_EQUIVALENT),
            "$KEY_ROWID = ?",
            arrayOf(part1Id.toString()),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            hasLong(0, -9000)
        }
    }

    /**
     * Tests that no excessive equivalent_amounts rows are created even when equivalentAmount is supplied:
     * 1. Split parts (part1Id, part2Id) must NOT have rows in equivalent_amounts.
     * 2. Home currency (USD) transactions must NOT have rows in equivalent_amounts.
     * 3. Static foreign currency (EUR, dynamic = false) transactions must NOT have rows in equivalent_amounts.
     */
    @Test
    fun testNoExcessiveEquivalentAmountsForSplitPartsOrHomeCurrencyAccount() {
        val db =
            (
                contentResolver.acquireContentProviderClient(
                    TransactionProvider.AUTHORITY
                )!!.localContentProvider as TransactionProvider
                ).openHelperForTest.writableDatabase

        // 1. Verify equivalent_amounts table contents for the EUR split transaction:
        // Only the parent split transaction (splitParentId) should have a row.
        db.query(table = TABLE_EQUIVALENT_AMOUNTS, columns = arrayOf(KEY_TRANSACTIONID))
            .useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(0, splitParentId)
            }

        // 2. Verify home currency account transaction (attempting non-null equivalentAmount insert):
        val usdAccountId = insertAccount("USD Account", currency = "USD")
        val usdTrans = repository.insertTransaction(usdAccountId, -5000, equivalentAmount = -5000)

        db.query(
            table = TABLE_EQUIVALENT_AMOUNTS,
            columns = arrayOf(KEY_TRANSACTIONID),
            selection = "$KEY_TRANSACTIONID = ?",
            selectionArgs = arrayOf(usdTrans.id.toString())
        ).useAndAssert {
            hasCount(0)
        }

        // 3. Verify static foreign currency account transaction (dynamic = false, attempting non-null equivalentAmount insert):
        val staticEurAccountId =
            insertAccount("Static EUR Account", currency = "EUR", dynamic = false)
        val staticEurTrans =
            repository.insertTransaction(staticEurAccountId, -5000, equivalentAmount = -7500)

        db.query(
            table = TABLE_EQUIVALENT_AMOUNTS,
            columns = arrayOf(KEY_TRANSACTIONID),
            selection = "$KEY_TRANSACTIONID = ?",
            selectionArgs = arrayOf(staticEurTrans.id.toString())
        ).useAndAssert {
            hasCount(0)
        }
    }

    /**
     * Tests that database migration upgradeTo193() deletes invalid equivalent_amounts rows:
     * - Rows for actual split parts (whose parent is a split transaction, cat_id = 0)
     * - Rows for home currency accounts (currency == homeCurrency)
     * AND preserves valid rows for:
     * - Legitimate zero equivalent amount rows in foreign accounts (e.g. small foreign payment rounding to 0)
     * - Top-level split parents (parent_id IS NULL, cat_id = 0)
     * - Archived ordinary transactions (parent_id points to an archive row, cat_id != 0)
     * - Archived split parents (parent_id points to an archive row, cat_id == 0)
     */
    @Test
    fun testUpgradeTo193CleansUpExcessiveEquivalentAmounts() {
        val db =
            (
                contentResolver.acquireContentProviderClient(
                    TransactionProvider.AUTHORITY
                )!!.localContentProvider as TransactionProvider
                ).openHelperForTest.writableDatabase

        // 1. Setup accounts and transactions
        val usdAccountId = insertAccount("USD Account Migration Test", currency = "USD")
        val usdTrans = repository.insertTransaction(usdAccountId, -5000)

        val zeroTrans =
            repository.insertTransaction(foreignAccountId, -1, categoryId = expenseCategory1)

        // Create an archived ordinary transaction in dynamic EUR account
        val archivedOrdinaryTrans =
            repository.insertTransaction(foreignAccountId, -8000, categoryId = expenseCategory1)
        val archiveRowId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.TRANSACTIONS_URI,
                ContentValues().apply {
                    put(KEY_ACCOUNTID, foreignAccountId)
                    put(KEY_AMOUNT, -8000)
                    put(KEY_DATE, System.currentTimeMillis() / 1000)
                    put(KEY_VALUE_DATE, System.currentTimeMillis() / 1000)
                    put(KEY_STATUS, STATUS_ARCHIVE)
                    put(KEY_CR_STATUS, "UNRECONCILED")
                    put(KEY_UUID, generateUuid())
                }
            )!!
        )
        // Mark ordinary transaction as archived (parent_id = archiveRowId, status = STATUS_ARCHIVED)
        contentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,
            ContentValues().apply {
                put(KEY_PARENTID, archiveRowId)
                put(KEY_STATUS, STATUS_ARCHIVED)
            },
            "$KEY_ROWID = ?",
            arrayOf(archivedOrdinaryTrans.id.toString())
        )

        // Create an archived split parent transaction in dynamic EUR account
        val archivedSplitParent = repository.createSplitTransaction(
            Transaction(
                accountId = foreignAccountId,
                amount = -12000,
                categoryId = SPLIT_CATID,
                equivalentAmount = -18000,
                uuid = generateUuid()
            ),
            listOf(
                Transaction(
                    accountId = foreignAccountId,
                    amount = -12000,
                    categoryId = expenseCategory1,
                    uuid = generateUuid()
                )
            )
        )
        // Mark split parent as archived (parent_id = archiveRowId, status = STATUS_ARCHIVED)
        contentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,
            ContentValues().apply {
                put(KEY_PARENTID, archiveRowId)
                put(KEY_STATUS, STATUS_ARCHIVED)
            },
            "$KEY_ROWID = ?",
            arrayOf(archivedSplitParent.data.id.toString())
        )

        // 2. Forcefully insert rows into equivalent_amounts
        // Invalid: split child part
        db.insert(
            TABLE_EQUIVALENT_AMOUNTS,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
            ContentValues(3).apply {
                put(KEY_TRANSACTIONID, part1Id)
                put(KEY_EQUIVALENT_AMOUNT, -9000)
                put(KEY_CURRENCY, "USD")
            }
        )
        // Legitimate zero equivalent amount row for small foreign payment
        db.insert(
            TABLE_EQUIVALENT_AMOUNTS,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
            ContentValues(3).apply {
                put(KEY_TRANSACTIONID, zeroTrans.id)
                put(KEY_EQUIVALENT_AMOUNT, 0)
                put(KEY_CURRENCY, "USD")
            }
        )
        // Invalid: home currency transaction
        db.insert(
            TABLE_EQUIVALENT_AMOUNTS,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
            ContentValues(3).apply {
                put(KEY_TRANSACTIONID, usdTrans.id)
                put(KEY_EQUIVALENT_AMOUNT, -5000)
                put(KEY_CURRENCY, "USD")
            }
        )
        // Valid: archived ordinary transaction equivalent amount
        db.insert(
            TABLE_EQUIVALENT_AMOUNTS,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
            ContentValues(3).apply {
                put(KEY_TRANSACTIONID, archivedOrdinaryTrans.id)
                put(KEY_EQUIVALENT_AMOUNT, -12000)
                put(KEY_CURRENCY, "USD")
            }
        )

        // Verify total equivalent_amounts count prior to upgradeTo193
        // Should be: splitParentId + archivedSplitParent.id + archivedOrdinaryTrans.id + zeroTrans.id + 2 invalid rows = 6 rows
        db.query(table = TABLE_EQUIVALENT_AMOUNTS, columns = arrayOf(KEY_TRANSACTIONID))
            .useAndAssert {
                hasCount(6)
            }

        // 3. Execute upgradeTo193()
        with(TransactionDatabase(application, prefHandler)) {
            db.upgradeTo193()
        }

        // 4. Verify that only the 2 invalid rows (part1Id, usdTrans.id) were deleted, leaving 4 valid rows
        db.query(table = TABLE_EQUIVALENT_AMOUNTS, columns = arrayOf(KEY_TRANSACTIONID))
            .useAndAssert {
                hasCount(4)
            }

        // Verify specifically that zeroTrans still has its row
        db.query(
            table = TABLE_EQUIVALENT_AMOUNTS,
            columns = arrayOf(KEY_TRANSACTIONID),
            selection = "$KEY_TRANSACTIONID = ?",
            selectionArgs = arrayOf(zeroTrans.id.toString())
        ).useAndAssert {
            hasCount(1)
        }

        // Verify specifically that archivedOrdinaryTrans and archivedSplitParent still have their rows
        db.query(
            table = TABLE_EQUIVALENT_AMOUNTS,
            columns = arrayOf(KEY_TRANSACTIONID),
            selection = "$KEY_TRANSACTIONID = ?",
            selectionArgs = arrayOf(archivedOrdinaryTrans.id.toString())
        ).useAndAssert {
            hasCount(1)
        }

        db.query(
            table = TABLE_EQUIVALENT_AMOUNTS,
            columns = arrayOf(KEY_TRANSACTIONID),
            selection = "$KEY_TRANSACTIONID = ?",
            selectionArgs = arrayOf(archivedSplitParent.data.id.toString())
        ).useAndAssert {
            hasCount(1)
        }
    }

    /**
     * Tests that an archived ordinary transaction (whose parent_id points to an archive row)
     * uses its own stored equivalent amount and is NOT treated as a split part.
     */
    @Test
    fun testArchivedOrdinaryTransactionUsesOwnEquivalentAmountNotArchiveRate() {
        // Create an ordinary transaction with custom stored equivalent amount = -7000 (-70.00 USD)
        val trans =
            repository.insertTransaction(foreignAccountId, -5000, categoryId = expenseCategory1)
        val db =
            (
                contentResolver.acquireContentProviderClient(
                    TransactionProvider.AUTHORITY
                )!!.localContentProvider as TransactionProvider
                ).openHelperForTest.writableDatabase

        db.insert(
            TABLE_EQUIVALENT_AMOUNTS,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
            ContentValues(3).apply {
                put(KEY_TRANSACTIONID, trans.id)
                put(KEY_EQUIVALENT_AMOUNT, -7000)
                put(KEY_CURRENCY, "USD")
            }
        )

        // Create an archive container row (status = STATUS_ARCHIVE = 4) with amount = -100000
        val archiveRowId = ContentUris.parseId(
            contentResolver.insert(
                TransactionProvider.TRANSACTIONS_URI,
                ContentValues().apply {
                    put(KEY_ACCOUNTID, foreignAccountId)
                    put(KEY_AMOUNT, -100000)
                    put(KEY_DATE, System.currentTimeMillis() / 1000)
                    put(KEY_VALUE_DATE, System.currentTimeMillis() / 1000)
                    put(KEY_STATUS, STATUS_ARCHIVE)
                    put(KEY_CR_STATUS, "UNRECONCILED")
                    put(KEY_UUID, generateUuid())
                }
            )!!
        )

        // Archive the transaction (set parent_id = archiveRowId, status = STATUS_ARCHIVED = 5)
        contentResolver.update(
            TransactionProvider.TRANSACTIONS_URI,
            ContentValues().apply {
                put(KEY_PARENTID, archiveRowId)
                put(KEY_STATUS, STATUS_ARCHIVED)
            },
            "$KEY_ROWID = ?",
            arrayOf(trans.id.toString())
        )

        // Query KEY_AMOUNT_HOME_EQUIVALENT for trans.id
        contentResolver.query(
            TransactionProvider.EXTENDED_URI,
            arrayOf(KEY_AMOUNT_HOME_EQUIVALENT),
            "$KEY_ROWID = ?",
            arrayOf(trans.id.toString()),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
            // Must return trans's OWN stored equivalent amount (-7000), NOT calculated from archive parent
            hasLong(0, -7000)
        }
    }

    /**
     * Tests global DYNAMIC setting override on a static account (dynamic = false).
     * With global setting DYNAMIC, even static accounts use dynamic calculations based on
     * stored equivalent amounts (split proportion: -20000 * 60/100 = -12000 USD) instead of
     * fixed rate results (1.50 * -6000 = -9000 USD).
     * Checks all 4 code paths:
     * 1. Home-equivalent projection (KEY_AMOUNT_HOME_EQUIVALENT on EXTENDED_URI)
     * 2. Grouped totals (groupingUriBuilder)
     * 3. Transaction sums (TRANSACTIONS_SUM_URI)
     * 4. Debt calculations (DEBTS_URI)
     */
    @Test
    fun testGlobalDynamicOverrideWithStaticAccount() {
        runBlocking {
            dataStore.edit {
                it[dynamicExchangeRatesDefaultKey] = "DYNAMIC"
            }
        }
        try {
            val staticAccountId =
                insertAccount("Static EUR Test Account", currency = "EUR", dynamic = false)
            val split = repository.createSplitTransaction(
                Transaction(
                    accountId = staticAccountId,
                    amount = -10000,
                    categoryId = SPLIT_CATID,
                    equivalentAmount = -20000,
                    uuid = generateUuid()
                ),
                listOf(
                    Transaction(
                        accountId = staticAccountId,
                        amount = -6000,
                        categoryId = expenseCategory1,
                        equivalentAmount = -12000,
                        uuid = generateUuid()
                    ),
                    Transaction(
                        accountId = staticAccountId,
                        amount = -4000,
                        categoryId = expenseCategory2,
                        equivalentAmount = -8000,
                        uuid = generateUuid()
                    )
                )
            )
            val staticPart1Id = split.splitParts!![0].data.id

            val db =
                (
                    contentResolver.acquireContentProviderClient(
                        TransactionProvider.AUTHORITY
                    )!!.localContentProvider as TransactionProvider
                    ).openHelperForTest.writableDatabase
            db.insert(
                TABLE_ACCOUNT_EXCHANGE_RATES,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues(4).apply {
                    put(KEY_ACCOUNTID, staticAccountId)
                    put(KEY_CURRENCY_SELF, "EUR")
                    put(KEY_CURRENCY_OTHER, "USD")
                    put(KEY_EXCHANGE_RATE, 1.50)
                }
            )

            val staticDebtId = ContentUris.parseId(
                contentResolver.insert(
                    TransactionProvider.DEBTS_URI,
                    Debt(
                        0,
                        "Static Account Debt",
                        "",
                        payeeId,
                        100000,
                        CurrencyUnit.DebugInstance,
                        System.currentTimeMillis() / 1000
                    ).toContentValues()
                )!!
            )
            contentResolver.update(
                TransactionProvider.TRANSACTIONS_URI,
                ContentValues(1).apply { put(KEY_DEBT_ID, staticDebtId) },
                "$KEY_ROWID = ?",
                arrayOf(staticPart1Id.toString())
            )

            // 1. Home-equivalent projection for split part 1
            contentResolver.query(
                TransactionProvider.EXTENDED_URI,
                arrayOf(KEY_AMOUNT_HOME_EQUIVALENT),
                "$KEY_ROWID = ?",
                arrayOf(staticPart1Id.toString()),
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(0, -12000)
            }

            // 2. Grouped totals
            val groupUri = BaseTransactionProvider.groupingUriBuilder(Grouping.NONE).build()
            contentResolver.query(
                groupUri,
                null,
                "$KEY_ACCOUNTID = $staticAccountId",
                null,
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(actual.getColumnIndexOrThrow(KEY_SUM_EXPENSES), -20000)
            }

            // 3. Transaction sums
            val sumUri = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL, "true")
                .build()
            contentResolver.query(
                sumUri,
                arrayOf(KEY_SUM_EXPENSES),
                "$KEY_ACCOUNTID = $staticAccountId",
                null,
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(0, -20000)
            }

            // 4. Debt calculations
            val debtUri = TransactionProvider.DEBTS_URI.buildUpon().build()
            contentResolver.query(
                debtUri,
                null,
                "$TABLE_DEBTS.$KEY_ROWID = ?",
                arrayOf(staticDebtId.toString()),
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(actual.getColumnIndexOrThrow(KEY_EQUIVALENT_SUM), -12000)
            }
        } finally {
            runBlocking {
                dataStore.edit {
                    it.remove(dynamicExchangeRatesDefaultKey)
                }
            }
        }
    }

    /**
     * Tests global STATIC setting override on a dynamic account (dynamic = true).
     * With global setting STATIC, even dynamic accounts ignore stored equivalent amounts and
     * use fixed exchange rate results (1.50 * -6000 = -9000 USD, 1.50 * -10000 = -15000 USD).
     * Checks all 4 code paths:
     * 1. Home-equivalent projection (KEY_AMOUNT_HOME_EQUIVALENT on EXTENDED_URI)
     * 2. Grouped totals (groupingUriBuilder)
     * 3. Transaction sums (TRANSACTIONS_SUM_URI)
     * 4. Debt calculations (DEBTS_URI)
     */
    @Test
    fun testGlobalStaticOverrideWithDynamicAccount() {
        runBlocking {
            dataStore.edit {
                it[dynamicExchangeRatesDefaultKey] = "STATIC"
            }
        }
        try {
            val dynamicAccountId =
                insertAccount("Dynamic EUR Test Account", currency = "EUR", dynamic = true)
            val split = repository.createSplitTransaction(
                Transaction(
                    accountId = dynamicAccountId,
                    amount = -10000,
                    categoryId = SPLIT_CATID,
                    equivalentAmount = -20000,
                    uuid = generateUuid()
                ),
                listOf(
                    Transaction(
                        accountId = dynamicAccountId,
                        amount = -6000,
                        categoryId = expenseCategory1,
                        equivalentAmount = -12000,
                        uuid = generateUuid()
                    ),
                    Transaction(
                        accountId = dynamicAccountId,
                        amount = -4000,
                        categoryId = expenseCategory2,
                        equivalentAmount = -8000,
                        uuid = generateUuid()
                    )
                )
            )
            val dynamicPart1Id = split.splitParts!![0].data.id

            val db =
                (
                    contentResolver.acquireContentProviderClient(
                        TransactionProvider.AUTHORITY
                    )!!.localContentProvider as TransactionProvider
                    ).openHelperForTest.writableDatabase
            db.insert(
                TABLE_ACCOUNT_EXCHANGE_RATES,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                ContentValues(4).apply {
                    put(KEY_ACCOUNTID, dynamicAccountId)
                    put(KEY_CURRENCY_SELF, "EUR")
                    put(KEY_CURRENCY_OTHER, "USD")
                    put(KEY_EXCHANGE_RATE, 1.50)
                }
            )

            val dynamicDebtId = ContentUris.parseId(
                contentResolver.insert(
                    TransactionProvider.DEBTS_URI,
                    Debt(
                        0,
                        "Dynamic Account Debt",
                        "",
                        payeeId,
                        100000,
                        CurrencyUnit.DebugInstance,
                        System.currentTimeMillis() / 1000
                    ).toContentValues()
                )!!
            )
            contentResolver.update(
                TransactionProvider.TRANSACTIONS_URI,
                ContentValues(1).apply { put(KEY_DEBT_ID, dynamicDebtId) },
                "$KEY_ROWID = ?",
                arrayOf(dynamicPart1Id.toString())
            )

            // 1. Home-equivalent projection for split part 1
            contentResolver.query(
                TransactionProvider.EXTENDED_URI,
                arrayOf(KEY_AMOUNT_HOME_EQUIVALENT),
                "$KEY_ROWID = ?",
                arrayOf(dynamicPart1Id.toString()),
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(0, -9000)
            }

            // 2. Grouped totals
            val groupUri = BaseTransactionProvider.groupingUriBuilder(Grouping.NONE).build()
            contentResolver.query(
                groupUri,
                null,
                "$KEY_ACCOUNTID = $dynamicAccountId",
                null,
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(actual.getColumnIndexOrThrow(KEY_SUM_EXPENSES), -15000)
            }

            // 3. Transaction sums
            val sumUri = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL, "true")
                .build()
            contentResolver.query(
                sumUri,
                arrayOf(KEY_SUM_EXPENSES),
                "$KEY_ACCOUNTID = $dynamicAccountId",
                null,
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(0, -15000)
            }

            // 4. Debt calculations
            val debtUri = TransactionProvider.DEBTS_URI.buildUpon().build()
            contentResolver.query(
                debtUri,
                null,
                "$TABLE_DEBTS.$KEY_ROWID = ?",
                arrayOf(dynamicDebtId.toString()),
                null
            ).useAndAssert {
                hasCount(1)
                movesToFirst()
                hasLong(actual.getColumnIndexOrThrow(KEY_EQUIVALENT_SUM), -9000)
            }
        } finally {
            runBlocking {
                dataStore.edit {
                    it.remove(dynamicExchangeRatesDefaultKey)
                }
            }
        }
    }
}
