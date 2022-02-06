package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.After
import org.totschnig.myexpenses.activity.TestMyExpenses
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.DecoratedCheckSealedHandler

abstract class BaseMyExpensesCabTest : BaseUiTest<TestMyExpenses>() {
    private lateinit var activityScenario: ActivityScenario<TestMyExpenses>

    override val testScenario: ActivityScenario<out TestMyExpenses>
        get() = activityScenario

    private val countingResource = CountingIdlingResource("CheckSealed")

    fun launch(id: Long) {
        activityScenario = ActivityScenario.launch(
            Intent(
                targetContext,
                TestMyExpenses::class.java
            ).apply {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            })
        activityScenario.onActivity { activity: TestMyExpenses ->
            activity.decoratedCheckSealedHandler =
                DecoratedCheckSealedHandler(activity.contentResolver, countingResource)
        }
        IdlingRegistry.getInstance().register(countingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(countingResource)
    }
}