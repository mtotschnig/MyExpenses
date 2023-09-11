package org.totschnig.myexpenses.db2

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import androidx.annotation.VisibleForTesting
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.TransactionProvider

fun Repository.createParty(party: Party) = contentResolver.createParty(party)
fun Repository.createParty(party: String) = contentResolver.createParty(Party.create(name = party))

fun Repository.saveParty(party: Party) {
    contentResolver.update(
        ContentUris.withAppendedId(TransactionProvider.PAYEES_URI, party.id),
        party.asContentValues,
        null,
        null
    )
}

/**
 * Looks for a party by name and iban
 * @return id or null if not found
 */
fun Repository.findParty(party: Party) = contentResolver.findParty(party.name, party.iban)

fun Repository.findParty(name: String) = contentResolver.findParty(name)
fun Repository.requireParty(name: String) = contentResolver.requireParty(name)


// legacy methods with ContentResolver receivier
fun ContentResolver.requireParty(name: String): Long {
    return findParty(name) ?: createParty(Party.create(name = name)).id
}

fun ContentResolver.createParty(party: Party) = party.copy(
    id = ContentUris.parseId(
        insert(
            TransactionProvider.PAYEES_URI,
            party.asContentValues
        )!!
    )
)

fun ContentResolver.findParty(party: String, iban: String? = null) = query(
    TransactionProvider.PAYEES_URI,
    arrayOf(DatabaseConstants.KEY_ROWID),
    KEY_PAYEE_NAME + " = ? AND " + KEY_IBAN + if (iban == null) " IS NULL" else " = ?",
    if (iban == null) arrayOf(party.trim()) else arrayOf(party, iban),
    null
)?.use { if (it.moveToFirst()) it.getLong(0) else null }

fun Repository.unsetParentId(partyId: Long) {
    setParentId(partyId, null)
}

@VisibleForTesting
fun Repository.setParentId(partyId: Long, parentId: Long?) {
    contentResolver.update(
        ContentUris.withAppendedId(TransactionProvider.PAYEES_URI, partyId),
        ContentValues(1).apply { put(KEY_PARENTID, parentId) },
        null, null
    )
}


