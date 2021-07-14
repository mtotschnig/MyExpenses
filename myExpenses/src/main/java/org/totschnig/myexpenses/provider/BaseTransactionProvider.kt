package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CURRENCIES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED

internal const val CURRENCIES_USAGES_TABLE_EXPRESSION =
    "$TABLE_CURRENCIES LEFT JOIN (SELECT coalesce($KEY_ORIGINAL_CURRENCY, $KEY_CURRENCY) AS currency_coalesced, count(*) AS $KEY_USAGES FROM $VIEW_EXTENDED GROUP BY currency_coalesced) on currency_coalesced = $KEY_CODE"

abstract class BaseTransactionProvider: ContentProvider() {
    var dirty = false
    set(value) {
        if(!field && value) {
            (context?.applicationContext as? MyApplication)?.markDataDirty()
        }
        field = value
    }
}