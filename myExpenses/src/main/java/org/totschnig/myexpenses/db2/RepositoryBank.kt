package org.totschnig.myexpenses.db2

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMap

fun Repository.loadBanks(): Flow<List<Bank>> {
    return contentResolver.observeQuery(
        uri = TransactionProvider.BANKS_URI,
        notifyForDescendants = true
    ).mapToList { Bank.fromCursor(it) }
}

fun Repository.loadBank(bankId: Long) = contentResolver.query(
    TransactionProvider.BANKS_URI,
    null,
    "$KEY_ROWID = ?",
    arrayOf(bankId.toString()),
    null
)!!.use {
    it.moveToFirst()
    Bank.fromCursor(it)
}


fun Repository.createBank(bank: Bank): Bank {
    val id = ContentUris.parseId(
        contentResolver.insert(TransactionProvider.BANKS_URI, ContentValues().apply {
            put(KEY_BLZ, bank.blz)
            put(KEY_BIC, bank.bic)
            put(KEY_BANK_NAME, bank.bankName)
            put(KEY_USER_ID, bank.userId)
        })!!
    )
    return bank.copy(id = id)
}

fun Repository.deleteBank(id: Long) {
    contentResolver.delete(
        ContentUris.withAppendedId(TransactionProvider.BANKS_URI, id),
        null,
        null
    )
}

@SuppressLint("Recycle")
fun Repository.importedAccounts(bankId: Long) = contentResolver.query(
    TransactionProvider.ACCOUNTS_URI,
    arrayOf(DatabaseConstants.KEY_ACCOUNT_NUMBER),
    "$KEY_BANK_ID = ?",
    arrayOf(bankId.toString()),
    null
)?.useAndMap { it.getString(0) } ?: emptyList()