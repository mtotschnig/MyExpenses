package org.totschnig.myexpenses.next.merchants

import android.content.ContentResolver
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.TransactionProvider.PAYEES_URI
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.next.data.merchants.RawPayee
import org.totschnig.myexpenses.next.feature.merchants.MerchantLogos

/** The payees of the user for [MerchantLogos], read again on each change */
fun ContentResolver.rawPayees(): Flow<List<RawPayee>> =
    observeQuery(
        uri = PAYEES_URI,
        projection = arrayOf(KEY_ROWID, KEY_PAYEE_NAME, KEY_SHORT_NAME, KEY_PARENTID),
    ).mapToList {
        RawPayee(
            it.getLong(KEY_ROWID), it.getString(KEY_PAYEE_NAME), it.getStringOrNull(KEY_SHORT_NAME), it.getLongOrNull(KEY_PARENTID)
        )
    }
