package org.totschnig.myexpenses.test.espresso

import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.AccountEdit
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.getUuidForAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.dynamicExchangeRatesDefaultKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.testutils.BaseUiTest
import org.totschnig.myexpenses.testutils.Espresso.wait
import org.totschnig.myexpenses.testutils.TestShard1
import org.totschnig.myexpenses.testutils.cleanup
import org.totschnig.myexpenses.testutils.withAccountType
import org.totschnig.myexpenses.testutils.withCurrency
import org.totschnig.myexpenses.testutils.withListSize
import org.totschnig.myexpenses.viewmodel.data.Currency

@TestShard1
class AccountEditTest : BaseUiTest<AccountEdit>() {

    private fun launch(id: Long? = null) {
        val i = Intent(targetContext, AccountEdit::class.java).apply {
            if (id != null) {
                putExtra(DatabaseConstants.KEY_ROWID, id)
            }
        }
        testScenario = ActivityScenario.launchActivityForResult(i)
    }

    @Test
    fun saveAccount() {
        launch()
        onView(withId(R.id.Currency)).check(matches(isDisplayed()))
        onView(withId(R.id.Currency)).perform(wait(withListSize(greaterThan(0)), 1000))
        onView(withId(R.id.Label)).perform(ViewActions.typeText(LABEL), closeSoftKeyboard())
        clickFab()
        assertFinishing()
        assertThat(repository.findAnyOpenByLabel(LABEL)).isNotNull()
        cleanup {
            deleteAccount(LABEL)
        }
    }

    @Test
    fun shouldSetExcludeFromTotals() {
        launch()
        assertOverflowItemChecked(R.id.EXCLUDE_FROM_TOTALS_COMMAND, false)
        clickMenuItem(R.id.EXCLUDE_FROM_TOTALS_COMMAND)
        assertOverflowItemChecked(R.id.EXCLUDE_FROM_TOTALS_COMMAND, true)
    }

    @Test
    fun shouldSetDynamicExchangeRate() {
        launch()
        setCurrency("VND")
        assertOverflowItemChecked(R.id.DYNAMIC_EXCHANGE_RATE_COMMAND, false)
        clickMenuItem(R.id.DYNAMIC_EXCHANGE_RATE_COMMAND)
        assertOverflowItemChecked(R.id.DYNAMIC_EXCHANGE_RATE_COMMAND, true)
    }

    @Test
    fun shouldNotHaveDynamicExchangeRateMenuItemIfGloballySet() {
        runBlocking {
            dataStore.edit {
                it[dynamicExchangeRatesDefaultKey] = "DYNAMIC"
            }
        }
        launch()
        setCurrency("VND")
        assertMenuItemHidden(R.id.DYNAMIC_EXCHANGE_RATE_COMMAND)
    }

    @Test
    fun shouldKeepUuidAfterSave() {
        val (id, uuid) = with(buildAccount(LABEL)) {
            id to uuid
        }
        launch(id)
        clickFab()
        assertThat(repository.getUuidForAccount(id)).isEqualTo(uuid)
        cleanup {
            deleteAccount(LABEL)
        }
    }

    @Test
    fun shouldNotShowDiscardDialogWithoutChanges() {
        launch()
        pressBackUnconditionally()
        assertCanceled()
    }

    @Test
    fun shouldKeepStateAfterRotation() {
        launch()
        setCurrency("VND")
        setAccountType(AccountType.INVESTMENT.name)
        doWithRotation {
            onView(withId(R.id.Currency)).check(matches(withSpinnerText(java.util.Currency.getInstance("VND").displayName)))
            onView(withId(R.id.AccountType)).check(matches(withSpinnerText(AccountType.INVESTMENT.name)))
        }
    }

    private fun setCurrency(currency: String) {
        BaristaClickInteractions.clickOn(R.id.Currency)
        onData(
            allOf(
                instanceOf(Currency::class.java),
                withCurrency(currency)
            )
        ).perform(click())
    }

    private fun setAccountType(accountType: String) {
        BaristaClickInteractions.clickOn(R.id.AccountType)
        onData(
            withAccountType(accountType)
        ).perform(click())
    }

    companion object {
        private const val LABEL = "Test account"
    }
}