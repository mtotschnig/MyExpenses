package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.testutils.BaseUiTest

abstract class BaseExpenseEditTest: BaseUiTest<TestExpenseEdit>() {
    lateinit var account1: Account

    val intentForNewTransaction
        get() = intent.apply {
            putExtra(KEY_ACCOUNTID, account1.id)
        }

    val intent get() = Intent(targetContext, TestExpenseEdit::class.java)
}