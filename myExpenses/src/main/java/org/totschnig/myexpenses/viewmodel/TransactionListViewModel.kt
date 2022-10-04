package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.saveTagLinks
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Tag

class TransactionListViewModel(application: Application) : BudgetViewModel(application) {

    companion object {
        fun prefNameForCriteria(accountId: Long) = "filter_%s_${accountId}"
    }
}

