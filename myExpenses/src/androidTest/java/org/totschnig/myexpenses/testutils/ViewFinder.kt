package org.totschnig.myexpenses.testutils


import android.view.View
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import org.hamcrest.StringDescription

fun withViewCount(matcher: Matcher<View>, expectedCount: Int): ViewAssertion {
    return ViewAssertion { view, noViewException ->
        if (noViewException != null) {
            // If noViewException is not null, it means Espresso's onView() didn't
            // find any views matching its root matcher. In our case, we use isRoot(),
            // so this should not happen. But if it does, we re-throw.
            throw noViewException
        }

        // We find all matching views in the hierarchy starting from the root.
        val views = mutableListOf<View>()
        for (child in TreeIterables.breadthFirstViewTraversal(view.rootView)) {
            if (matcher.matches(child)) {
                views.add(child)
            }
        }

        val actualCount = views.size
        if (actualCount != expectedCount) {
            val description = StringDescription()
            description.appendText("Found $actualCount views with matcher: ")
            matcher.describeTo(description)
            description.appendText(" but expected $expectedCount.")
            throw AssertionError(description.toString())
        }
    }
}
