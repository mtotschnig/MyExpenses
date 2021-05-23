package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import android.os.Parcelable
import androidx.lifecycle.liveData
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter

@Parcelize
data class PlanInstanceInfo(val templateId: Long, val instanceId: Long? = null, val date: Long? = null, val transactionId: Long? = null): Parcelable

class TemplatesListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun updateDefaultAction(itemIds: LongArray, action: Template.Action) = liveData(context = coroutineContext()) {
        emit(contentResolver.update(TransactionProvider.TEMPLATES_URI,
                ContentValues().apply { put(DatabaseConstants.KEY_DEFAULT_ACTION, action.name) },
                DatabaseConstants.KEY_ROWID + " " + WhereFilter.Operation.IN.getOp(itemIds.size),
                itemIds.map(Long::toString).toTypedArray()) == itemIds.size)
    }

    fun newFromTemplate(plans: Array<PlanInstanceInfo>) = liveData(context = coroutineContext()) {
        emit(plans.map { plan ->
            Transaction.getInstanceFromTemplateWithTags(plan.templateId)?.let {
                val (t, tagList) = it
                if (plan.date != null) {
                    t.date = plan.date
                    t.valueDate = plan.date
                    t.originPlanInstanceId = plan.instanceId
                }
                t.status = DatabaseConstants.STATUS_NONE
                t.save(true) != null && t.saveTags(tagList)
            }
        }.sumBy { if (it == true) 1 else 0 })
    }
}