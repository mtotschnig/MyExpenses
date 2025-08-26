package org.totschnig.myexpenses.testutils

import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withParentIndex
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.adapter.SpinnerItem
import org.totschnig.myexpenses.delegate.TransactionDelegate.OperationType
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.ui.DateButton
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import java.time.LocalDate

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

fun withOperationType(type: Int): Matcher<Any> =
    object : BoundedMatcher<Any, OperationType>(OperationType::class.java) {
        override fun matchesSafely(myObj: OperationType): Boolean {
            return myObj.type == type
        }

        override fun describeTo(description: Description) {
            description.appendText("with operation type '$type'")
        }
    }

fun withCurrency(currency: String): Matcher<Any> =
    object : BoundedMatcher<Any, Currency>(Currency::class.java) {
        override fun matchesSafely(myObj: Currency): Boolean {
            return myObj.code == currency
        }

        override fun describeTo(description: Description) {
            description.appendText("with currency '$currency'")
        }
    }

fun withAccountType(expectedTypeName: String) =
    object : TypeSafeMatcher<SpinnerItem.Item<AccountType>>() {
        override fun describeTo(description: Description) {
            description.appendText("SpinnerItem.Item<AccountType>: '$expectedTypeName'")
        }

        override fun matchesSafely(item: SpinnerItem.Item<AccountType>): Boolean {
            return item.data.name == expectedTypeName
        }
    }

fun withAccount(content: String): Matcher<Any> =
    object : BoundedMatcher<Any, IdHolder>(IdHolder::class.java) {
        override fun matchesSafely(myObj: IdHolder): Boolean {
            return myObj.toString() == content
        }

        override fun describeTo(description: Description) {
            description.appendText("with label '$content'")
        }
    }


fun withAccountGrouped(expectedAccount: String): Matcher<SpinnerItem.Item<Account>> =
    object : TypeSafeMatcher<SpinnerItem.Item<Account>>() {
        override fun describeTo(description: Description) {
            description.appendText("SpinnerItem.Item<Account>: '$expectedAccount'")
        }

        override fun matchesSafely(item: SpinnerItem.Item<Account>): Boolean {
            return item.data.label == expectedAccount
        }
    }

/**
 * https://stackoverflow.com/a/63330069/1199911
 * @param parentViewId the resource id of the parent [View].
 * @param position the child index of the [View] to match.
 * @return a [Matcher] that matches the child [View] which has the given [position] within the specified parent.
 */
fun withPositionInParent(parentViewId: Int, position: Int): Matcher<View> =
    allOf(withParent(withId(parentViewId)), withParentIndex(position))

fun toolbarMainTitle(): ViewInteraction =
    onView(withIdAndAncestor(R.id.title, R.id.toolbar))

fun toolbarMainSubtitle(): ViewInteraction =
    onView(withIdAndAncestor(R.id.subtitle, R.id.toolbar))

fun toolbarTitle(): ViewInteraction =
    onView(allOf(instanceOf(TextView::class.java), withParent(withId(R.id.toolbar))))

//Espresso recorder
fun childAtPosition(parentMatcher: Matcher<View>, position: Int) =
    object : TypeSafeMatcher<View>() {
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


//https://google.github.io/android-testing-support-library/docs/espresso/advanced/#asserting-that-a-data-item-is-not-in-an-adapter
fun withAdaptedData(dataMatcher: Matcher<out Any>) = object : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("with class name: ")
        dataMatcher.describeTo(description)
    }

    public override fun matchesSafely(view: View): Boolean {
        if (view !is AdapterView<*>) {
            return false
        }

        val adapter = view.adapter
        for (i in 0 until adapter.count) {
            if (dataMatcher.matches(adapter.getItem(i))) {
                return true
            }
        }

        return false
    }
}

// Credits: http://stackoverflow.com/a/30361345/1199911
@Suppress("unused")
fun withListSize(size: Int) = withListSize(`is`(size))

fun withListSize(integerMatcher: Matcher<Int>) =
    object : BoundedMatcher<View, AdapterView<*>>(AdapterView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("with number: ")
            integerMatcher.describeTo(description)
        }

        public override fun matchesSafely(adapterView: AdapterView<*>): Boolean {
            return integerMatcher.matches(adapterView.adapter.count)
        }
    }

fun withIdAndParent(id: Int, parentId: Int): Matcher<View> =
    allOf(withId(id), withParent(withId(parentId)))

fun withIdAndAncestor(id: Int, parentId: Int): Matcher<View> =
    allOf(withId(id), isDescendantOfA(withId(parentId)))

fun withChain(id: Int, ancestor1: Int, ancestor2: Int) =
    allOf(
        withId(id),
        isDescendantOfA(
            allOf(
                withId(ancestor1),
                isDescendantOfA(withId(ancestor2))
            )
        )
    )

fun dateButtonHasDate(expectedDate: LocalDate): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("DateButton has date: ")
            description?.appendValue(expectedDate.toString())
        }

        override fun matchesSafely(item: View?): Boolean {
            if (item !is DateButton) {
                return false
            }
            return item.date == expectedDate
        }
    }
}
