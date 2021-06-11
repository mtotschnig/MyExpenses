package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED

internal const val CURRENCIES_USAGES =
    "(SELECT count(*) from $VIEW_EXTENDED where $KEY_CURRENCY = $KEY_CODE OR $KEY_ORIGINAL_CURRENCY = $KEY_CODE) as $KEY_USAGES"

abstract class BaseTransactionProvider: ContentProvider() {
    var dirty = false
    set(value) {
        if(!field && value) {
            (context?.applicationContext as? MyApplication)?.markDataDirty()
        }
        field = value
    }
}