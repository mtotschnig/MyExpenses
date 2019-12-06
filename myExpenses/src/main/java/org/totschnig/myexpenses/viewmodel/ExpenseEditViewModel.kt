package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod

class ExpenseEditViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private val methods = MutableLiveData<List<PaymentMethod>>()

    fun getMethods(): LiveData<List<PaymentMethod>> {
        return methods
    }

    fun loadMethods(isIncome: Boolean, type: AccountType) {
        disposable = briteContentResolver.createQuery(TransactionProvider.METHODS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                .appendPath(if (isIncome) "1" else "-1")
                .appendQueryParameter(QUERY_PARAMETER_ACCOUNTY_TYPE_LIST, type.name)
                .build(), null, null, null, null, false)
                .mapToList { PaymentMethod.create(it) }
                .subscribe { methods.postValue(it) }
    }
}
