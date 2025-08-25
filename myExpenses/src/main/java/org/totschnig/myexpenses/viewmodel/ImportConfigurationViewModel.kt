package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal

open class ImportConfigurationViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
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
                        id = 0,
                        label = getString(R.string.menu_create_account) + " / " + getString(R.string.read_from_data),
                        currency = currencyContext.homeCurrencyString,
                        type = null,
                        flag = null
                    )
                )
                addAll(it)
            }
        }
}