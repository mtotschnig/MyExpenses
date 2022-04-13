package org.totschnig.myexpenses.test.espresso

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.*
import java.time.ZonedDateTime
import java.util.*

class RemapAccountTest : BaseMyExpensesCabTest() {
    private lateinit var account1: Account
    private lateinit var account2: Account
    private lateinit var account3: Account
    private lateinit var transfer: Transfer

    val currencyUnit = CurrencyUnit.DebugInstance

    private fun createAccount(label: String): Account = Account(
        label, currencyUnit, 0, "", AccountType.CASH, Account.DEFAULT_COLOR
    ).also {
        it.save()
    }

    private fun createMoney() = Money(currencyUnit, 2000)

    @Before
    fun fixture() {
        account1 = createAccount("K1")
        account2 = createAccount("K2")
        account3 = createAccount("K3")
        Transaction(account1.id, createMoney()).also {
            it.setDate(ZonedDateTime.now().plusDays(4))
            it.save()
        }
        transfer = Transfer(account1.id, createMoney(), account2.id).also {
            it.save()
        }
        launch(account1.id)
    }

    @Test
    fun remapAccountShouldUpdateTransferPeer() {
        waitForAdapter()
        openCab()
        clickMenuItem(R.id.REMAP_PARENT, true)
        onView(allOf(withText(R.string.account))).perform(click())
        onView(withText("K3")).perform(click())
        //Espresso recorder

        onView(
            Matchers.allOf(
                ViewMatchers.withId(android.R.id.button1), withText(android.R.string.ok),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        ).perform(ViewActions.scrollTo(), click())

        onView(
            Matchers.allOf(
                ViewMatchers.withId(android.R.id.button1), withText(R.string.menu_remap),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        ).perform(ViewActions.scrollTo(), click())
        val self = Transaction.getInstanceFromDb(transfer.id)
        Truth.assertThat(self.accountId).isEqualTo(account3.id)
        Truth.assertThat(self.transferAccountId).isEqualTo(account2.id)
        val peer = Transaction.getInstanceFromDb(transfer.transferPeer!!)
        Truth.assertThat(peer.accountId).isEqualTo(account2.id)
        Truth.assertThat(peer.transferAccountId).isEqualTo(account3.id)
    }

    //Espresso recorder
    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}