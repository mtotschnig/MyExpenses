package org.totschnig.myexpenses.provider

import android.content.ContentResolver
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI

fun ContentResolver.triggerAccountListRefresh() {
    notifyChange(ACCOUNTS_URI, null, false)
}