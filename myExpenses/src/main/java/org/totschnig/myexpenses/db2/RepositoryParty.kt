package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.content.ContentValues
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils

fun Repository.createParty(party: Party) = party.copy(
    id = ContentUris.parseId(
        contentResolver.insert(
            TransactionProvider.PAYEES_URI,
            party.asContentValues
        )!!
    )
)

fun Repository.saveParty(party: Party) {
    contentResolver.update(
        ContentUris.withAppendedId(TransactionProvider.PAYEES_URI, party.id),
        party.asContentValues,
        null,
        null
    )
}

/**
 * check if a party exists, create it if not
 * @return id of the existing or the new party
 */
fun Repository.requireParty(party: Party) = findParty(party) ?: createParty(party).id

/**
 * Looks for a party by name and iban
 * @return id or null if not found
 */
fun Repository.findParty(party: Party) = contentResolver.query(
    TransactionProvider.PAYEES_URI,
    arrayOf(DatabaseConstants.KEY_ROWID),
    KEY_PAYEE_NAME + " = ? AND " + KEY_IBAN + if (party.iban == null) " IS NULL" else " = ?",
    if (party.iban == null) arrayOf(party.name) else arrayOf(party.name, party.iban),
    null
)?.use { if (it.moveToFirst()) it.getLong(0) else null }
