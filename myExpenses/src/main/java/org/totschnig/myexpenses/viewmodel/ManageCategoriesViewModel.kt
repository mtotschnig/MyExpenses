package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.provider.TransactionProvider

class ManageCategoriesViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    fun importCats() = liveData(context = coroutineContext()) {
        emit(
            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_SETUP_CATEGORIES,
                null,
                null
            )?.getInt(TransactionProvider.KEY_RESULT) ?:0
        )
    }
}