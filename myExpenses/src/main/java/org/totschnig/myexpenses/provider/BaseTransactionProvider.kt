package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import org.totschnig.myexpenses.MyApplication

abstract class BaseTransactionProvider: ContentProvider() {
    var dirty = false
    set(value) {
        if(field == false && value == true) {
            (context?.applicationContext as? MyApplication)?.markDataDirty()
        }
        field = value
    }
}