package org.totschnig.myexpenses.viewmodel

import android.app.Application
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.DateCriteria
import org.totschnig.myexpenses.ui.DiscoveryHelper
import timber.log.Timber

class UpgradeHandlerViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun upgrade(fromVersion: Int, toVersion: Int) {
        if (fromVersion < 385) {
            val hasIncomeColumn = "max(amount * (transfer_peer is null)) > 0 "
            val hasTransferOrSplitColumn = "not(max(parent_id) is null and max(transfer_peer) is null)"
            val projection = arrayOf(hasIncomeColumn, hasTransferOrSplitColumn)
            disposable = briteContentResolver.createQuery(TransactionProvider.TRANSACTIONS_URI,
                    projection, null, null, null, false)
                    .subscribe { query ->
                        query.run()?.let { cursor ->
                            if (cursor.moveToFirst()) {
                                val discoveryHelper = getApplication<MyApplication>().appComponent.discoveryHelper()
                                if (cursor.getInt(0) > 0) {
                                    discoveryHelper.markDiscovered(DiscoveryHelper.Feature.EI_SWITCH)
                                }
                                if (cursor.getInt(1) > 0) {
                                    discoveryHelper.markDiscovered(DiscoveryHelper.Feature.OPERATION_TYPE_SELECT)
                                }
                            }
                            cursor.close()
                        }
                    }
        }
        if (fromVersion < 391) {
            val dateFilterList = MyApplication.getInstance().settings.all.entries.map { it.key }.filter { it.startsWith("filter_date") }
            val prefHandler = getApplication<MyApplication>().appComponent.prefHandler()
            dateFilterList.forEach { key ->
                prefHandler.getString(key, null)?.let { legacy ->
                    try {
                        DateCriteria.fromLegacy(legacy).toStringExtra().also { new ->
                            prefHandler.putString(key, new)
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                        prefHandler.remove(key)
                    }
                }
            }
        }
        if (fromVersion < 393) {
            val prefHandler = getApplication<MyApplication>().appComponent.prefHandler()
            arrayOf(PrefKey.SORT_ORDER_ACCOUNTS, PrefKey.SORT_ORDER_CATEGORIES, PrefKey.SORT_ORDER_BUDGET_CATEGORIES).forEach {
                if (Sort.TITLE.name.equals(prefHandler.getString(it, null))) {
                    prefHandler.putString(it, Sort.LABEL.name)
                }
            }
        }
    }
}