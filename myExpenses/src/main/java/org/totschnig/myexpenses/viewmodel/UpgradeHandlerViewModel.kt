package org.totschnig.myexpenses.viewmodel

import android.app.Application
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.DiscoveryHelper

class UpgradeHandlerViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun upgrade(fromVersion:Int, toVersion: Int) {
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
    }
}