package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getEnum

data class TemplateInfo(val rowId: Long, val title: String, val defaultAction: Template.Action) {
    companion object {
        fun fromTemplate(template: Template) =
            TemplateInfo(template.id, template.title, template.defaultAction)
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
