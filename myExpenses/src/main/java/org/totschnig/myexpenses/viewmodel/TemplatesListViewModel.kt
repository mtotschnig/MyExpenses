package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter

class TemplatesListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun updateDefaultAction(itemIds: LongArray, action: Template.Action) = liveData(context = coroutineContext()) {
        emit(contentResolver.update(TransactionProvider.TEMPLATES_URI,
                ContentValues().apply { put(DatabaseConstants.KEY_DEFAULT_ACTION, action.name) },
                DatabaseConstants.KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(itemIds.size),
                itemIds.map(Long::toString).toTypedArray()) == itemIds.size)
    }

    fun newFromTemplate(itemIds: LongArray, extraInfo: Array<Array<Long>>?) = liveData(context = coroutineContext()) {
        emit(itemIds.mapIndexed { index, id ->
            Transaction.getInstanceFromTemplateWithTags(id)?.let {
                val (t, tagList) = it
                if (extraInfo != null) {
                    val date: Long = extraInfo.get(index).get(1) / 1000
                    t.date = date
                    t.valueDate = date
                    t.originPlanInstanceId = extraInfo.get(index).get(0)
                }
                t.status = DatabaseConstants.STATUS_NONE
                t.save(true) != null && t.saveTags(tagList, contentResolver)
            }
        }.sumBy { if (it == true) 1 else 0 })
    }
}