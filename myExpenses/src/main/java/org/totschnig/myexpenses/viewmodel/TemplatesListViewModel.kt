package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import android.os.Parcelable
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState

@Parcelize
data class PlanInstanceInfo(
    val templateId: Long,
    val instanceId: Long? = null,
    val date: Long? = null,
    val transactionId: Long? = null,
    val state: PlanInstanceState = PlanInstanceState.OPEN
) : Parcelable

class TemplatesListViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    fun updateDefaultAction(itemIds: LongArray, action: Template.Action) =
        liveData(context = coroutineContext()) {
            emit(
                contentResolver.update(
                    TransactionProvider.TEMPLATES_URI,
                    ContentValues().apply {
                        put(
                            DatabaseConstants.KEY_DEFAULT_ACTION,
                            action.name
                        )
                    },
                    "${DatabaseConstants.KEY_ROWID} IN (${itemIds.joinToString()})", null
                ) == itemIds.size
            )
        }

    fun newFromTemplate(plans: Array<out PlanInstanceInfo>) =
        liveData(context = coroutineContext()) {
            emit(plans.map { plan ->
                Transaction.getInstanceFromTemplateWithTags(plan.templateId)?.let {
                    val (t, tagList) = it
                    if (plan.date != null) {
                        val date = plan.date / 1000
                        t.date = date
                        t.valueDate = date
                        t.originPlanInstanceId = plan.instanceId
                    }
                    t.status = DatabaseConstants.STATUS_NONE
                    t.save(true) != null && t.saveTags(tagList)
                }
            }.sumBy { if (it == true) 1 else 0 })
        }

    fun reset(instances: Array<out PlanInstanceInfo>) {
        viewModelScope.launch(coroutineContext()) {
            instances.forEach { instance ->
                instance.transactionId?.let {
                    Transaction.delete(it, false)
                }
                contentResolver.delete(
                    TransactionProvider.PLAN_INSTANCE_SINGLE_URI(
                        instance.templateId,
                        instance.instanceId!!
                    ), null, null
                )
            }
        }
    }

    fun cancel(instances: Array<out PlanInstanceInfo>) {
        viewModelScope.launch(coroutineContext()) {
            instances.forEach { instance ->
                instance.transactionId?.let {
                    Transaction.delete(it, false)
                }
                contentResolver.insert(
                    TransactionProvider.PLAN_INSTANCE_STATUS_URI,
                    ContentValues(3).apply {
                        putNull(KEY_TRANSACTIONID)
                        put(KEY_TEMPLATEID, instance.templateId)
                        put(KEY_INSTANCEID, instance.instanceId!!)
                    })
            }
        }
    }
}