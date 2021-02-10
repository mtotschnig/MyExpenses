package org.totschnig.myexpenses.db2

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import java.math.BigDecimal
import javax.inject.Inject

class Repository(val contentResolver: ContentResolver, val currencyContext: CurrencyContext) {
    @Inject constructor(context: Context, currencyContext: CurrencyContext) : this(context.contentResolver, currencyContext)

    //Transaction
    fun createTransaction(transaction: Transaction) = with(transaction) {
        getCurrencyUnitForAccount(account)?.let { currencyUnit ->
            contentResolver.insert(TransactionProvider.TRANSACTIONS_URI, ContentValues().apply {
                put(DatabaseConstants.KEY_ACCOUNTID, account)
                put(DatabaseConstants.KEY_AMOUNT, Money(currencyUnit, BigDecimal(amount.toString())).amountMinor)
                val toEpochSecond = ZonedDateTime.of(date, LocalTime.now(), ZoneId.systemDefault()).toEpochSecond()
                put(DatabaseConstants.KEY_DATE, toEpochSecond)
                put(DatabaseConstants.KEY_VALUE_DATE, toEpochSecond)
                put(DatabaseConstants.KEY_PAYEEID, findOrWritePayee(payee))
                category.takeIf { it > 0 }?.let { put(DatabaseConstants.KEY_CATID, it) }
                put(DatabaseConstants.KEY_COMMENT, comment)
            })?.let { ContentUris.parseId(it) }
        }
    }

    //Payee
    fun findOrWritePayee(name: String) = findPayee(name) ?: createPayee(name)

    private fun findPayee(name: String) = contentResolver.query(TransactionProvider.PAYEES_URI,
            arrayOf(DatabaseConstants.KEY_ROWID),
            DatabaseConstants.KEY_PAYEE_NAME + " = ?",
            arrayOf(name.trim()), null)?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    }

    private fun createPayee(name: String) =
            contentResolver.insert(TransactionProvider.PAYEES_URI, ContentValues().apply {
                put(DatabaseConstants.KEY_PAYEE_NAME, name)
                put(DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
            })?.let { ContentUris.parseId(it) }

    //Account
    fun getCurrencyUnitForAccount(accountId: Long) =
            contentResolver.query(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
                    arrayOf(DatabaseConstants.KEY_CURRENCY), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }?.let { currencyContext[it] }
}