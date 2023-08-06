package org.totschnig.myexpenses.db2

import android.content.ContentValues
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getString

fun Repository.loadBanks(): Flow<List<Bank>> {
    return contentResolver.observeQuery(
        TransactionProvider.BANKS_URI,
        null, null, null, null
    ).mapToList {
        Bank(
            it.getString(KEY_BLZ),
            it.getString(KEY_BIC),
            it.getString(KEY_BANK_NAME),
            it.getString(KEY_USER_ID)
        )
    }
}

fun Repository.addBank(bank: Bank) {
    contentResolver.insert(TransactionProvider.BANKS_URI, ContentValues().apply {
        put(KEY_BLZ, bank.blz)
        put(KEY_BIC, bank.bic)
        put(KEY_BANK_NAME, bank.bankName)
        put(KEY_USER_ID, bank.userId)
    })
}