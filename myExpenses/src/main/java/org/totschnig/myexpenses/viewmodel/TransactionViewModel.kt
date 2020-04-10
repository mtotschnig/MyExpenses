package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.ProviderUtils
import org.totschnig.myexpenses.viewmodel.data.Tag
import javax.inject.Inject

open class TransactionViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher

    @Inject
    lateinit var prefHandler: PrefHandler

    protected val tags = MutableLiveData<MutableList<Tag>>()

    fun getTags(): LiveData<MutableList<Tag>> {
        return tags
    }

    enum class InstantiationTask { TRANSACTION, TEMPLATE, TRANSACTION_FROM_TEMPLATE, FROM_INTENT_EXTRAS }

    fun transaction(transactionId: Long, task: InstantiationTask, clone: Boolean, forEdit: Boolean, extras: Bundle?): LiveData<Transaction?> = liveData(context = coroutineContext()) {
        val transaction: Transaction?
        var tagList: List<Tag>? = null
        when (task) {
            InstantiationTask.TEMPLATE -> transaction = Template.getInstanceFromDb(transactionId)
            InstantiationTask.TRANSACTION_FROM_TEMPLATE -> with(Transaction.getInstanceFromTemplate(transactionId)) {
                transaction = first
                tagList = second
            }
            InstantiationTask.TRANSACTION -> transaction = Transaction.getInstanceFromDb(transactionId)
            InstantiationTask.FROM_INTENT_EXTRAS -> transaction = ProviderUtils.buildFromExtras(extras)
        }
        transaction?.let {
            if (forEdit) {
                it.prepareForEdit(clone, clone && prefHandler.getBoolean(PrefKey.CLONE_WITH_CURRENT_DATE, true))
            }
        }
        emit(transaction)
        tagList?.takeIf { it.size > 0 }?.let { tags.postValue(it.toMutableList()) }
    }

    fun loadOriginalTags(transaction: ITransaction) {
       loadOriginalTags(transaction.id, transaction.linkedTagsUri(), transaction.linkColumn())
    }
    fun loadOriginalTags(id: Long, uri: Uri, column: String) {
        disposable = briteContentResolver.createQuery(uri, null, column + " = ?", arrayOf(id.toString()), null, false)
                .mapToList { cursor ->
                    Tag(cursor.getLong(cursor.getColumnIndex(DatabaseConstants.KEY_ROWID)), cursor.getString(cursor.getColumnIndex(DatabaseConstants.KEY_LABEL)), true)
                }
                .subscribe { tags.postValue(it) }
    }

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher
}