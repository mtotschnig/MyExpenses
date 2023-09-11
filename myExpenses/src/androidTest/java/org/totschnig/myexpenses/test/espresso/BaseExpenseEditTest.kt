package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.TestExpenseEdit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso
import org.totschnig.myexpenses.testutils.Espresso.withIdAndParent

abstract class BaseExpenseEditTest: BaseUiTest<TestExpenseEdit>() {
    lateinit var account1: Account

    val intentForNewTransaction
        get() = intent.apply {
            putExtra(KEY_ACCOUNTID, account1.id)
        }

    val intent get() = Intent(targetContext, TestExpenseEdit::class.java)

    fun setAmount(amount: Int) {
        onView(
            withIdAndParent(
                R.id.AmountEditText,
                R.id.Amount
            )
        ).perform(
            scrollTo(),
            click(),
            replaceText(amount.toString())
        )
        closeSoftKeyboard()
    }
}