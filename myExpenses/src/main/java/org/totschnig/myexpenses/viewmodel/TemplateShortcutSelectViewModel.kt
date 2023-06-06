package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.TransactionProvider

class TemplateShortcutSelectViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    val templates =
        contentResolver.observeQuery(
            uri = TransactionProvider.TEMPLATES_URI,
            projection = arrayOf(KEY_ROWID, KEY_TITLE),
            selection = "$KEY_PARENTID IS null AND $KEY_PLANID IS null",
        )
            .mapToList { it.getLong(0) to it.getString(1) }
    .stateIn(viewModelScope, SharingStarted.Lazily, null)
}
