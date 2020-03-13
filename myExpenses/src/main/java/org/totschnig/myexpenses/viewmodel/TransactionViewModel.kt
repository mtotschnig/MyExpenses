package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.ProviderUtils
import javax.inject.Inject

open class TransactionViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher

    enum class InstantiationTask { TRANSACTION, TEMPLATE, TRANSACTION_FROM_TEMPLATE, FROM_INTENT_EXTRAS }

    fun transaction(transactionId: Long, task: InstantiationTask, clone: Boolean, forEdit: Boolean, extras: Bundle?): LiveData<Transaction?> = liveData(context = coroutineContext()) {
        emit(when (task) {
            InstantiationTask.TEMPLATE -> Template.getInstanceFromDb(transactionId)
            InstantiationTask.TRANSACTION_FROM_TEMPLATE -> Transaction.getInstanceFromTemplate(transactionId)
            InstantiationTask.TRANSACTION -> Transaction.getInstanceFromDb(transactionId)
            InstantiationTask.FROM_INTENT_EXTRAS -> ProviderUtils.buildFromExtras(extras)
        }?.also {
            if (forEdit) {
                it.prepareForEdit(clone)
            }
        })
    }

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher
}