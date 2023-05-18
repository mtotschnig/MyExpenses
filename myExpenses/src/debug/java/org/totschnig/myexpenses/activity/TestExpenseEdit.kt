package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.os.Bundle
import org.totschnig.myexpenses.viewmodel.data.Account

class TestExpenseEdit: ExpenseEdit() {
    private var activityIsRecreated = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            activityIsRecreated = true
        }
        if (android.os.Build.VERSION.SDK_INT < 23) {
            onEnterAnimationComplete()
        }
    }

    @SuppressLint("VisibleForTests")
    override fun setAccounts(accounts: List<Account>) {
        super.setAccounts(accounts)
        if (activityIsRecreated) {
            activityIsRecreated = false
        }
    }
}