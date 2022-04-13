package org.totschnig.myexpenses.test.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageCategories
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso
import java.util.concurrent.TimeoutException

class ManageCategoriesTest : BaseUiTest<ManageCategories>() {
    @get:Rule
    var scenarioRule = ActivityScenarioRule(
        ManageCategories::class.java
    )

    @Test
    @Throws(TimeoutException::class)
    fun setupCategoriesShouldPopulateList() {
        assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isEqualTo(0)
        Espresso.openActionBarOverflowMenu()
        androidx.test.espresso.Espresso.onView(ViewMatchers.withText(R.string.menu_categories_setup_default))
            .perform(ViewActions.click())
        assertThat(repository.count(TransactionProvider.CATEGORIES_URI)).isGreaterThan(0)
    }

    override val testScenario: ActivityScenario<ManageCategories>
        get() = scenarioRule.scenario
}