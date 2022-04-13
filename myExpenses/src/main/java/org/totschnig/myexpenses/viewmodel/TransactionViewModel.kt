package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.ProviderUtils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Tag
import javax.inject.Inject

open class TransactionViewModel(application: Application) : TagHandlingViewModel(application) {

    enum class InstantiationTask { TRANSACTION, TEMPLATE, TRANSACTION_FROM_TEMPLATE, FROM_INTENT_EXTRAS }

    fun transaction(transactionId: Long, task: InstantiationTask, clone: Boolean, forEdit: Boolean, extras: Bundle?): LiveData<Transaction?> = liveData(context = coroutineContext()) {
        when (task) {
            InstantiationTask.TEMPLATE -> Template.getInstanceFromDbWithTags(transactionId)
            InstantiationTask.TRANSACTION_FROM_TEMPLATE -> Transaction.getInstanceFromTemplateWithTags(transactionId)
            InstantiationTask.TRANSACTION -> Transaction.getInstanceFromDbWithTags(transactionId)
            InstantiationTask.FROM_INTENT_EXTRAS -> Pair(ProviderUtils.buildFromExtras(repository, extras!!), emptyList())
        }?.also { pair ->
            if (forEdit) {
                pair.first.prepareForEdit(clone, clone && prefHandler.getBoolean(PrefKey.CLONE_WITH_CURRENT_DATE, true))
            }
            emit(pair.first)
            pair.second?.takeIf { it.size > 0 }?.let { tags.postValue(it.toMutableList()) }
        } ?: run {
            emit(null)
        }
    }

    fun loadOriginalTags(id: Long, uri: Uri, column: String) {
        disposable = briteContentResolver.createQuery(uri, null, "$column = ?", arrayOf(id.toString()), null, false)
                .mapToList { cursor ->
                    Tag(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_ROWID)), cursor.getString(cursor.getColumnIndexOrThrow(DatabaseConstants.KEY_LABEL)), true)
                }
                .subscribe { tags.postValue(it) }
    }
}