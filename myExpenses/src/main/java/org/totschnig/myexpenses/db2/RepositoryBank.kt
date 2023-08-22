package org.totschnig.myexpenses.db2

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CONTEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMap
import java.time.LocalDate

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

data class AccountInformation(val number: String?, val subnumber: String?, val iban: String?, val lastSynced: LocalDate?) {
    companion object {
        fun fromMap(map: Map<Attribute, String>) = AccountInformation(
            map[BankingAttribute.NUMBER],
            map[BankingAttribute.SUBNUMBER],
            map[BankingAttribute.IBAN],
            map[BankingAttribute.LAST_SYCNED_WITH_BANK]?.let { LocalDate.parse(it) }
        )
    }
}

@SuppressLint("Recycle")
fun Repository.importedAccounts(bankId: Long): List<AccountInformation> =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_ATTRIBUTES_URI,
        arrayOf(KEY_ACCOUNTID, KEY_CONTEXT, KEY_ATTRIBUTE_NAME, KEY_VALUE),
        "$KEY_ACCOUNTID IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_BANK_ID = ?) AND $KEY_CONTEXT = '${BankingAttribute.CONTEXT}'",
        arrayOf(bankId.toString()),
        KEY_ACCOUNTID
    )?.use { cursor ->
        cursor.asSequence.groupBy(
            keySelector = { it.getLong(0) },
            valueTransform = { Attribute.from(it) }
        )
    }?.map { (_, value) -> AccountInformation.fromMap(value.toMap()) } ?: emptyList()

fun Repository.accountInformation(accountId: Long): AccountInformation? = contentResolver.query(
    TransactionProvider.ACCOUNTS_ATTRIBUTES_URI,
    arrayOf(KEY_CONTEXT, KEY_ATTRIBUTE_NAME, KEY_VALUE),
    "$KEY_ACCOUNTID = ? AND $KEY_CONTEXT = '${BankingAttribute.CONTEXT}'",
    arrayOf(accountId.toString()),
    null
)?.useAndMap { cursor -> Attribute.from(cursor) }?.let {
    AccountInformation.fromMap(it.toMap())
}