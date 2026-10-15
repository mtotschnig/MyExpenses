package org.totschnig.myexpenses.test.provider

import android.content.ContentValues
import org.totschnig.myexpenses.provider.KEY_CODE
import org.totschnig.myexpenses.provider.KEY_FRACTION_DIGITS
import org.totschnig.myexpenses.provider.KEY_SYMBOL
import org.totschnig.myexpenses.provider.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.TransactionDatabase
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.update
import org.totschnig.myexpenses.testutils.BaseDbTest

class CurrencyMigrationTest : BaseDbTest() {
    fun testUpgradeTo189() {
        val currencyCode = "EUR"
        // 1. Setup: Ensure currency exists with NULL custom values
        val values = ContentValues().apply {
            putNull(KEY_SYMBOL)
            putNull(KEY_FRACTION_DIGITS)
        }
        mDb.update(TABLE_CURRENCIES, values, "$KEY_CODE = ?", arrayOf(currencyCode))

        // 2. Setup: Set legacy preferences
        prefHandler.putString(currencyCode + "CustomCurrencySymbol", "€€")
        prefHandler.putInt(currencyCode + "CustomFractionDigits", 3)

        // 3. Execution: Call upgradeTo189
        val transactionDatabase = TransactionDatabase(targetContext, prefHandler)
        with(transactionDatabase) {
            mDb.upgradeTo189()
        }

        // 4. Verification
        mDb.query("SELECT $KEY_SYMBOL, $KEY_FRACTION_DIGITS FROM $TABLE_CURRENCIES WHERE $KEY_CODE = ?", arrayOf(currencyCode)).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("€€", cursor.getString(KEY_SYMBOL))
            assertEquals(3, cursor.getInt(KEY_FRACTION_DIGITS))
        }
    }

    fun testUpgradeTo189ShouldNotOverwrite() {
        val currencyCode = "USD"
        // 1. Setup: Ensure currency exists WITH custom values already in DB
        val values = ContentValues().apply {
            put(KEY_SYMBOL, "$")
            put(KEY_FRACTION_DIGITS, 2)
        }
        mDb.update(TABLE_CURRENCIES, values, "$KEY_CODE = ?", arrayOf(currencyCode))

        // 2. Setup: Set legacy preferences to DIFFERENT values
        prefHandler.putString(currencyCode + "CustomCurrencySymbol", "US$")
        prefHandler.putInt(currencyCode + "CustomFractionDigits", 4)

        // 3. Execution: Call upgradeTo189
        val transactionDatabase = TransactionDatabase(targetContext, prefHandler)
        with(transactionDatabase) {
            mDb.upgradeTo189()
        }

        // 4. Verification: Should still be old values
        mDb.query("SELECT $KEY_SYMBOL, $KEY_FRACTION_DIGITS FROM $TABLE_CURRENCIES WHERE $KEY_CODE = ?", arrayOf(currencyCode)).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("$", cursor.getString(KEY_SYMBOL))
            assertEquals(2, cursor.getInt(KEY_FRACTION_DIGITS))
        }
    }
}
