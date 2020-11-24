package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter

class TemplatesListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun updateDefaultAction(itemIds: LongArray, action: Template.Action) = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(contentResolver.update(TransactionProvider.TEMPLATES_URI,
                ContentValues().apply { put(DatabaseConstants.KEY_DEFAULT_ACTION, action.name) },
                DatabaseConstants.KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(itemIds.size),
                itemIds.map(Long::toString).toTypedArray()) == itemIds.size)
    }
}