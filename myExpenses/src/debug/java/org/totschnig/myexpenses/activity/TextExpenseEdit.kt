package org.totschnig.myexpenses.activity

import android.os.Bundle
import org.totschnig.myexpenses.viewmodel.data.Account

class TestExpenseEdit: ExpenseEdit() {
    var splitPartListUpdateCalled = 0
    private var activityIsRecreated = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            activityIsRecreated = true
        }
    }

    override fun updateSplitPartList(account: Account, rowId: Long) {
        super.updateSplitPartList(account, rowId)
        if (activityIsRecreated) {
            activityIsRecreated = false
        } else {
            splitPartListUpdateCalled++
        }
    }
}