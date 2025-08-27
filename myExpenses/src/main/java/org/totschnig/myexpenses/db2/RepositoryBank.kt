package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.content.ContentValues
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CONTEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VERSION
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.useAndMapToList
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
            put(KEY_VERSION, 2)
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

data class AccountInformation(
    val accountId: Long,
    val accountTypeId: Long,
    val name: String?,
    val blz: String?,
    val number: String?,
    val subnumber: String?,
    val iban: String?,
    val bic: String?,
    val lastSynced: LocalDate?
) {
    companion object {
        fun fromMap(accountId: Long, accountTypeId: Long, map: Map<Attribute, String>) = AccountInformation(
            accountId = accountId,
            accountTypeId = accountTypeId,
            name = map[BankingAttribute.NAME],
            blz = map[BankingAttribute.BLZ],
            number = map[BankingAttribute.NUMBER],
            subnumber = map[BankingAttribute.SUBNUMBER],
            iban = map[BankingAttribute.IBAN],
            bic = map[BankingAttribute.BIC],
            lastSynced = map[BankingAttribute.LAST_SYCNED_WITH_BANK]?.let { LocalDate.parse(it) }
        )
    }
}

fun Repository.importedAccounts(bankId: Long): List<AccountInformation> =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_URI,
        arrayOf(KEY_ROWID, KEY_TYPE),
        "$KEY_BANK_ID = ?",
        arrayOf(bankId.toString()),
        KEY_ROWID
    )?.useAndMapToList { cursor ->
      accountInformation(cursor.getLong(0), cursor.getLong(1))
    }?.filterNotNull() ?: emptyList()

//noinspection Recycle
fun Repository.accountInformation(accountId: Long, accountTypeId: Long): AccountInformation? = contentResolver.query(
    TransactionProvider.ACCOUNTS_ATTRIBUTES_URI,
    arrayOf(KEY_CONTEXT, KEY_ATTRIBUTE_NAME, KEY_VALUE),
    "$KEY_ACCOUNTID = ? AND $KEY_CONTEXT = '${BankingAttribute.CONTEXT}'",
    arrayOf(accountId.toString()),
    null
)?.useAndMapToList { cursor -> Attribute.from(cursor) }?.let {
    AccountInformation.fromMap(accountId, accountTypeId, it.toMap())
}