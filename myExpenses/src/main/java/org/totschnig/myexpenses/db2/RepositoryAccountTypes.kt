package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.content.Context
import android.database.DatabaseUtils
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.withAppendedId

fun Repository.getAccountTypes(): Flow<List<AccountType>> = contentResolver.observeQuery(
    TransactionProvider.ACCOUNT_TYPES_URI,
    notifyForDescendants = true,
).mapToList {
    AccountType.fromCursor(it)
}

fun Repository.loadAccountType(id: Long): AccountType = contentResolver.query(
    TransactionProvider.ACCOUNT_TYPES_URI.withAppendedId(id),
    null,
    null,
    null,
    null
)!!.use {
    it.moveToFirst()
    AccountType.fromCursor(it)
}

fun Repository.addAccountType(accountType: AccountType): AccountType {
    val id = ContentUris.parseId(
        contentResolver.insert(
            TransactionProvider.ACCOUNT_TYPES_URI,
            accountType.asContentValues
        )!!
    )
    return accountType.copy(id = id)
}

fun Repository.updateAccountType(accountType: AccountType) {
    require(accountType.id > 0)
    contentResolver.update(
        TransactionProvider.ACCOUNT_TYPES_URI.withAppendedId(accountType.id),
        accountType.asContentValues,
        null,
        null
    )
}

fun Repository.deleteAccountType(accountTypeId: Long) {
    contentResolver.delete(
        TransactionProvider.ACCOUNT_TYPES_URI.withAppendedId(accountTypeId),
        null,
        null
    )
}

fun Repository.findAccountType(name: String): AccountType? = contentResolver.query(
TransactionProvider.ACCOUNT_TYPES_URI,
null,
"$KEY_LABEL = ?",
arrayOf(name),
null
)?.use {
    if (it.moveToFirst()) {
        AccountType.fromCursor(it)
    } else null
}

fun localizedLabelForAccountType(ctx: Context, keyLabel: String) =
    StringBuilder().apply {
        append("CASE ").append(keyLabel)
        AccountType.predefinedAccounts.forEach {
            append(" WHEN '").append(it.name).append("' THEN ")
            DatabaseUtils.appendEscapedSQLString(this, it.localizedName(ctx))
        }
        append(" ELSE ").append(keyLabel).append(" END")
    }.toString()