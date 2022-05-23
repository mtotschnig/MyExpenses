package org.totschnig.myexpenses.testutils

import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withParent
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IAccount
import org.totschnig.myexpenses.delegate.TransactionDelegate
import org.totschnig.myexpenses.delegate.TransactionDelegate.OperationType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod

fun withMethod(label: String): Matcher<Any> =
        object : BoundedMatcher<Any, PaymentMethod>(PaymentMethod::class.java) {
            override fun matchesSafely(myObj: PaymentMethod): Boolean {
                return myObj.label().equals(label)
            }

            override fun describeTo(description: Description) {
                description.appendText("with method '${label}'")
            }
        }

fun withStatus(status: CrStatus): Matcher<Any> =
        object : BoundedMatcher<Any, CrStatus>(CrStatus::class.java) {
            override fun matchesSafely(myObj: CrStatus): Boolean {
                return myObj == status
            }

            override fun describeTo(description: Description) {
                description.appendText("with status '${status.name}'")
            }
        }

fun withAccount(content: String): Matcher<Any> =
        object : BoundedMatcher<Any, IAccount>(IAccount::class.java) {
            override fun matchesSafely(myObj: IAccount): Boolean {
                return myObj.toString() == content
            }

            override fun describeTo(description: Description) {
                description.appendText("with label '$content'")
            }
        }

fun withOperationType(type: Int): Matcher<Any> =
    object : BoundedMatcher<Any, OperationType>(OperationType::class.java) {
        override fun matchesSafely(myObj: OperationType): Boolean {
            return myObj.type == type
        }

        override fun describeTo(description: Description) {
            description.appendText("with operation type '$type'")
        }
    }

fun toolbarTitle(): ViewInteraction = onView(allOf(instanceOf(TextView::class.java), withParent(ViewMatchers.withId(R.id.toolbar))))