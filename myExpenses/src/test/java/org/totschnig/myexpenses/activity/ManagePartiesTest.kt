package org.totschnig.myexpenses.activity

import android.content.ContentResolver
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.clickMenu
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.provider.TransactionProvider

@RunWith(AndroidJUnit4::class)
class ManagePartiesTest {
    private lateinit var activityScenario: ActivityScenario<ManageParties>
    private var payee1 = "John Doe"
    private var payee2 = "Hinz Finz"

    private val contentResolver: ContentResolver
        get() = getInstrumentation().targetContext.contentResolver

    @Before
    fun setUp() {
        contentResolver.delete(TransactionProvider.PAYEES_URI, null, null)
    }

    @After
    fun tearDown() {
        if (::activityScenario.isInitialized) {
            activityScenario.close()
        }
    }

    private fun fixture(action: Action) {
        contentResolver.requireParty(payee1)
        contentResolver.requireParty(payee2)
        activityScenario = ActivityScenario.launch(
            Intent(getInstrumentation().targetContext, ManageParties::class.java).also {
                it.action = action.name
            }
        )
    }

    @Test
    fun testSearchManage() {
        testSearch(Action.MANAGE, 2)
    }

    @Test
    fun testSearchSelectMapping() {
        testSearch(Action.SELECT_MAPPING, 3)
    }

    @Test
    fun testSearchSelectFilter() {
        testSearch(Action.SELECT_FILTER, 3)
    }

    private fun waitForChildCount(expectedCount: Int, timeoutMs: Long = 2000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            getInstrumentation().waitForIdleSync()
            try {
                onView(withId(R.id.list))
                    .check(ViewAssertions.matches(ViewMatchers.hasChildCount(expectedCount)))
                return
            } catch (_: AssertionError) {
                Thread.sleep(50)
            }
        }
        onView(withId(R.id.list))
            .check(ViewAssertions.matches(ViewMatchers.hasChildCount(expectedCount)))
    }

    private fun testSearch(action: Action, expectedInitialCount: Int) {
        fixture(action)
        waitForChildCount(expectedInitialCount)
        clickMenu(R.id.SEARCH_COMMAND)
        onView(withId(androidx.appcompat.R.id.search_src_text)).perform(typeText("John"))
        waitForChildCount(expectedInitialCount - 1)
    }
}
