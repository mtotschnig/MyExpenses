package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal

open class ImportViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

    var accountId: Long
        get() = savedStateHandle.get<Long>(KEY_ACCOUNTID) ?: 0L
        set(value) {
            savedStateHandle[KEY_ACCOUNTID] = value
        }

    val accounts: Flow<List<AccountMinimal>>
        get() = accountsMinimal(withAggregates = false).map {
            buildList {
                add(
                    AccountMinimal(
                        0, getString(R.string.menu_create_account),
                        homeCurrencyProvider.homeCurrencyString,
                        AccountType.CASH
                    )
                )
                addAll(it)
            }
        }
}