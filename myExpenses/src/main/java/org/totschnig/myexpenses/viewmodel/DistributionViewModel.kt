package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo

class DistributionViewModel(application: Application, savedStateHandle: SavedStateHandle):
    DistributionViewModelBase<DistributionAccountInfo>(application, savedStateHandle) {
    private fun getGroupingPrefKey(accountId: Long) = stringPreferencesKey("distributionGrouping_$accountId")
    fun initWithAccount(accountId: Long, defaultGrouping: Grouping) {
        val base =
            if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        viewModelScope.launch(coroutineContext()) {
            contentResolver.query(ContentUris.withAppendedId(base, accountId),
            arrayOf(KEY_ROWID, KEY_LABEL, KEY_CURRENCY, KEY_COLOR), null, null, null)?.use {
                it.moveToFirst()
                _accountInfo.tryEmit(object: DistributionAccountInfo {
                    val label = it.getString(1)
                    override val accountId = it.getLong(0)
                    override fun label(context: Context) = label
                    override val currency = currencyContext.get(it.getString(2))
                    override val color = it.getInt(3)
                })
            }
        }
        viewModelScope.launch {
            dataStore.data.map {
                enumValueOrDefault(it[getGroupingPrefKey(accountId)], defaultGrouping)
            }.collect {
                setGrouping(it)
            }
        }
    }

    fun persistGrouping(grouping: Grouping) {
        accountInfo.value?.let {
            viewModelScope.launch {
                dataStore.edit { preference ->
                    preference[getGroupingPrefKey(it.accountId)] = grouping.name
                }
            }
        }
    }
}
