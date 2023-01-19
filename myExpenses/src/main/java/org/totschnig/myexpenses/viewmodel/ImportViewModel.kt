package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils

data class Account(override val id: Long, val label: String, val currency: String, val type: AccountType): IdHolder {
    override fun toString() = label
}

open class ImportViewModel(application: Application, val savedStateHandle: SavedStateHandle) : ContentResolvingAndroidViewModel(application) {

    var accountId: Long
        get() = savedStateHandle.get<Long>(KEY_ACCOUNTID) ?: 0L
        set(value) { savedStateHandle[KEY_ACCOUNTID] = value }

    val accounts: Flow<List<Account>>
        get() = contentResolver.observeQuery(
            TransactionProvider.ACCOUNTS_BASE_URI, arrayOf(
                DatabaseConstants.KEY_ROWID,
                DatabaseConstants.KEY_LABEL,
                DatabaseConstants.KEY_CURRENCY,
                DatabaseConstants.KEY_TYPE), DatabaseConstants.KEY_SEALED + " = 0 ", null,
            null, false
        )
            .mapToList {
                Account(
                    it.getLong(0),
                    it.getString(1),
                    it.getString(2),
                    AccountType.valueOf(it.getString(3))
                )
            }
            .map {
                buildList {
                    add(
                        Account(
                            0, getString(R.string.menu_create_account),
                            Utils.getHomeCurrency().code,
                            AccountType.CASH
                        )
                    )
                    addAll(it)
                }
            }
}