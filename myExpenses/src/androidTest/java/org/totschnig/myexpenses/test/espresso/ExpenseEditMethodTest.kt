package org.totschnig.myexpenses.test.espresso

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.insertTemplate
import org.totschnig.myexpenses.db2.insertTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.testutils.ACCOUNT_LABEL_1
import org.totschnig.myexpenses.testutils.BaseExpenseEditTest
import org.totschnig.myexpenses.testutils.TEMPLATE_TITLE
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.shared_test.TransactionData

class ExpenseEditMethodTest : BaseExpenseEditTest() {

    @Before
    fun baseFixture() {
        account1 = buildAccount(ACCOUNT_LABEL_1, type = AccountType.BANK)
    }

    @After
    fun clearDb() {
        cleanup {
            repository.deleteAccount(account1.id)
        }
    }

    @Test
    fun shouldSaveMethod() = runTest {

        launch()
        setAmount(101)
        setMethod(PreDefinedPaymentMethod.CREDITCARD)
        clickFab() // save transaction
        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = -10100,
                methodId = repository.findPaymentMethod(PreDefinedPaymentMethod.CREDITCARD.name)
            )
        )
    }

    @Test
    fun shouldLoadAndUpdateMethodWithOutlier() = runTest {

        val pm = repository.findPaymentMethod(PreDefinedPaymentMethod.CREDITCARD.name)
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = -100,
            methodId = pm
        )
        launch(getIntentForEditTransaction(transaction.id))

        runMethodCheck()

        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = 100,
                methodId = pm
            )
        )
    }

    @Test
    fun shouldLoadAndUpdateMethodWithOutlierTemplate() = runTest {

        val pm = repository.findPaymentMethod(PreDefinedPaymentMethod.CREDITCARD.name)
        val template = repository.insertTemplate(
            title = TEMPLATE_TITLE,
            accountId = account1.id,
            amount = -100,
            methodId = pm
        )
        launch(getIntentForEditTemplate(template.id))

        runMethodCheck()

        assertTemplate(
            expectedAccount = account1.id,
            expectedAmount = 100,
            expectedMethod = pm
        )
    }

    @Test
    fun shouldLoadAndUpdateMethodWithOutlierTransactionFromTemplate() = runTest {

        val pm = repository.findPaymentMethod(PreDefinedPaymentMethod.CREDITCARD.name)
        val template = repository.insertTemplate(
            title = TEMPLATE_TITLE,
            accountId = account1.id,
            amount = -100,
            methodId = pm
        )
        launch(getIntentForTransactionFromTemplate(template.id))

        runMethodCheck()

        assertTransaction(
            id = repository.loadTransactions(account1.id).first().id,
            TransactionData(
                accountId = account1.id,
                amount = 100,
                methodId = pm
            )
        )
    }

    @Test
    fun shouldLoadAndUpdateMethodWithOutlierTemplateFromTransaction() = runTest {

        val pm = repository.findPaymentMethod(PreDefinedPaymentMethod.CREDITCARD.name)
        val transaction = repository.insertTransaction(
            accountId = account1.id,
            amount = -100,
            methodId = pm
        )
        launch(getIntentForTemplateFromTransaction(transaction.id))
        checkToolbarTitleForTemplate()

        setTitle()
        runMethodCheck()

        assertTemplate(
            expectedAccount = account1.id,
            expectedAmount = 100,
            expectedMethod = pm
        )
    }

    private fun runMethodCheck() {
        val pm = PreDefinedPaymentMethod.CREDITCARD
        closeSoftKeyboard()
        checkMethod(pm)
        toggleType()
        //for an income creditcard is an outlier
        onView(withId(R.id.MethodSpinner)).check(
            matches(
                withEffectiveVisibility(
                    GONE
                )
            )
        )
        onView(withId(R.id.MethodOutlier)).check(
            matches(
                allOf(
                    withEffectiveVisibility(
                        ViewMatchers.Visibility.VISIBLE
                    ),
                    withText(getString(pm.resId))
                )
            )
        )
        clickFab() // save transaction
    }
}