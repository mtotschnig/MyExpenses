package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.provider.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PLANID
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_TITLE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getEnum

data class TemplateInfo(val rowId: Long, val title: String, val defaultAction: Template.Action) {
    companion object {
        fun fromTemplate(id: Long, title: String, defaultAction: Template.Action) =
            TemplateInfo(id, title, defaultAction)
    }
}

class TemplateShortcutSelectViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    val templates =
        contentResolver.observeQuery(
            uri = TransactionProvider.TEMPLATES_URI,
            projection = arrayOf(KEY_ROWID, KEY_TITLE, KEY_DEFAULT_ACTION),
            selection = "$KEY_PARENTID IS null AND $KEY_PLANID IS null",
        )
            .mapToList {
                TemplateInfo(
                    it.getLong(0),
                    it.getString(1),
                    it.getEnum(2, Template.Action.SAVE)
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
}
