package org.totschnig.myexpenses.testutils

import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.totschnig.myexpenses.adapter.IAccount
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod

fun withMethod(label: String): Matcher<Any> =
        object : BoundedMatcher<Any, PaymentMethod>(PaymentMethod::class.java) {
            override fun matchesSafely(myObj: PaymentMethod): Boolean {
                return myObj.label().equals(label)
            }

            override fun describeTo(description: Description) {
                description.appendText("with label '${label}'")
            }
        }

fun withStatus(status: Transaction.CrStatus): Matcher<Any> =
        object : BoundedMatcher<Any, Transaction.CrStatus>(Transaction.CrStatus::class.java) {
            override fun matchesSafely(myObj: Transaction.CrStatus): Boolean {
                return myObj.equals(status)
            }

            override fun describeTo(description: Description) {
                description.appendText("with label '${status.name}'")
            }
        }

fun withAccount(content: String): Matcher<Any> =
        object : BoundedMatcher<Any, IAccount>(IAccount::class.java) {
            override fun matchesSafely(myObj: IAccount): Boolean {
                return myObj.toString().equals(content)
            }

            override fun describeTo(description: Description) {
                description.appendText("with label '$content'")
            }
        }