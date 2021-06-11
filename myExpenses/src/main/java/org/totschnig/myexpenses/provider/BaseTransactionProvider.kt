package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS

internal const val CURRENCIES_USAGES =
    "(SELECT count(*) from $TABLE_ACCOUNTS where $KEY_CURRENCY = $KEY_CODE)  + (SELECT count(*) from $TABLE_TRANSACTIONS WHERE $KEY_ORIGINAL_CURRENCY = $KEY_CODE) as $KEY_USAGES"

abstract class BaseTransactionProvider: ContentProvider() {
    var dirty = false
    set(value) {
        if(!field && value) {
            (context?.applicationContext as? MyApplication)?.markDataDirty()
        }
        field = value
    }
}