package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Parcelable
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.instantiateTemplate
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.PLAN_INSTANCE_STATUS_URI
import org.totschnig.myexpenses.util.ExchangeRateHandler
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import javax.inject.Inject

@Parcelize
data class PlanInstanceInfo(
    val templateId: Long,
    val instanceId: Long? = null,
    val date: Long? = null,
    val transactionId: Long? = null,
    val state: PlanInstanceState = PlanInstanceState.OPEN,
) : Parcelable

class TemplatesListViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var exchangeRateHandler: ExchangeRateHandler

    fun updateDefaultAction(itemIds: LongArray, action: Template.Action) =
        liveData(context = coroutineContext()) {
            val result = contentResolver.update(
                TransactionProvider.TEMPLATES_URI,
                ContentValues().apply {
                    put(KEY_DEFAULT_ACTION, action.name)
                },
                "$KEY_ROWID IN (${itemIds.joinToString()})", null
            ) == itemIds.size
            val context = getApplication<MyApplication>()
            val shortcuts = ShortcutManagerCompat.getShortcuts(
                context,
                ShortcutManagerCompat.FLAG_MATCH_PINNED
            )
            if (result && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                itemIds.filter { itemId ->
                    shortcuts.any { it.id == ShortcutHelper.idTemplate(itemId) }
                }.forEach {
                    context.getSystemService(ShortcutManager::class.java).updateShortcuts(
                        listOf(
                            ShortcutInfo.Builder(context, ShortcutHelper.idTemplate(it))
                                .setIntent(
                                    ShortcutHelper.buildTemplateIntent(context, it, action)
                                )
                                .build()
                        )
                    )
                }
            }
            emit(result)
        }

    fun newFromTemplate(vararg plans: PlanInstanceInfo) =
        liveData(context = coroutineContext()) {
            emit(plans.map { plan ->
                instantiateTemplate(
                    repository,
                    exchangeRateHandler,
                    plan,
                    currencyContext.homeCurrencyUnit
                )
            }.sumBy { if (it == null) 0 else 1 })
        }

    fun reset(instance: PlanInstanceInfo) {
        viewModelScope.launch(coroutineContext()) {
            instance.transactionId?.let {
                repository.deleteTransaction(it)
            }
            contentResolver.delete(
                TransactionProvider.PLAN_INSTANCE_SINGLE_URI(
                    instance.templateId,
                    instance.instanceId!!
                ), null, null
            )
        }
    }

    fun cancel(instance: PlanInstanceInfo) {
        viewModelScope.launch(coroutineContext()) {
            instance.transactionId?.let {
                repository.deleteTransaction(it)
            }
            contentResolver.insert(
                PLAN_INSTANCE_STATUS_URI,
                ContentValues(3).apply {
                    putNull(KEY_TRANSACTIONID)
                    put(KEY_TEMPLATEID, instance.templateId)
                    put(KEY_INSTANCEID, instance.instanceId!!)
                })
        }
    }

    override fun deleteTransactions(ids: LongArray, markAsVoid: Boolean) {
        super.deleteTransactions(ids, markAsVoid)
        contentResolver.notifyChange(PLAN_INSTANCE_STATUS_URI, null, false)
    }

    fun relink(instance: PlanInstanceInfo, adjustDate: Boolean) {
        val whereArguments = arrayOf(instance.transactionId.toString())
        viewModelScope.launch(coroutineContext()) {
            val update = contentResolver.update(
                PLAN_INSTANCE_STATUS_URI,
                ContentValues(1).apply {
                    put(KEY_INSTANCEID, instance.instanceId)
                },
                "$KEY_TRANSACTIONID = ?", whereArguments
            )
            if (update == 1) {
                if (adjustDate) {
                    contentResolver.update(
                        TransactionProvider.TRANSACTIONS_URI,
                        ContentValues(1).apply {
                            put(KEY_DATE, instance.date!! / 1000)
                        },
                        "$KEY_ROWID = ?", whereArguments
                    )
                }
            } else {
                CrashHandler.report(
                    Exception("Expected 1 row to be affected by relink operation, actual $update")
                )
            }
        }
    }
}