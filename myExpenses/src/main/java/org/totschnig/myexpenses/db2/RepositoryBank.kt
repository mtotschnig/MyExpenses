package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.content.ContentValues
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

fun Repository.loadBanks(): Flow<List<Bank>> {
    return contentResolver.observeQuery(
        uri = TransactionProvider.BANKS_URI,
        notifyForDescendants = true
    ).mapToList {
        Bank(
            id = it.getLong(KEY_ROWID),
            blz= it.getString(KEY_BLZ),
            bic = it.getString(KEY_BIC),
            bankName = it.getString(KEY_BANK_NAME),
            userId = it.getString(KEY_USER_ID),
            count = it.getInt(KEY_COUNT)
        )
    }
}

fun Repository.createBank(bank: Bank): Bank {
    val id = ContentUris.parseId(
        contentResolver.insert(TransactionProvider.BANKS_URI, ContentValues().apply {
            put(KEY_BLZ, bank.blz)
            put(KEY_BIC, bank.bic)
            put(KEY_BANK_NAME, bank.bankName)
            put(KEY_USER_ID, bank.userId)
        })!!)
    return bank.copy(id = id)
}

fun Repository.deleteBank(id: Long) {
    contentResolver.delete(ContentUris.withAppendedId(TransactionProvider.BANKS_URI, id), null ,null)
}