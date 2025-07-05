package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.os.Bundle
import org.totschnig.myexpenses.viewmodel.data.Account

class TestExpenseEdit: ExpenseEdit() {

    override val helpContext = "ExpenseEdit"

    private var activityIsRecreated = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            activityIsRecreated = true
        }
    }

    @SuppressLint("VisibleForTests")
    override fun setAccounts(accounts: List<Account>, isInitialSetup: Boolean) {
        super.setAccounts(accounts, isInitialSetup)
        if (activityIsRecreated) {
            activityIsRecreated = false
        }
    }
}