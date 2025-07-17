package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class CalculatorInputTest {

    private lateinit var scenario: ActivityScenario<CalculatorInput>

    @After
    fun tearDown() {
        scenario.close()
    }

    private fun launchActivity(intent: Intent? = null) {
        scenario = if (intent == null) {
            ActivityScenario.launchActivityForResult(CalculatorInput::class.java)
        } else {
            ActivityScenario.launchActivityForResult(intent)
        }
        // Wait for activity to be created and started
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNumberInput_displaysCorrectly() {
        launchActivity()
        testHelper("123", R.id.b1, R.id.b2, R.id.b3)
    }

    @Test
    fun testDecimalInput_displaysCorrectly() {
        launchActivity()
        testHelper("1.5", R.id.b1, R.id.bDot, R.id.b5)
    }

    @Test
    fun testSimpleOperations_displaysCorrectResult() {
        launchActivity()
        // 12 + 5 = 17
        testHelper("17", R.id.b1, R.id.b2, R.id.bAdd, R.id.b5, R.id.bResult)
        // 12 - 5 = 7
        testHelper("7", R.id.b1, R.id.b2, R.id.bSubtract, R.id.b5, R.id.bResult)
        // 12 * 5 = 60
        testHelper("60", R.id.b1, R.id.b2, R.id.bMultiply, R.id.b5, R.id.bResult)
        // 12 % 5 = 2.4
        testHelper("2.4", R.id.b1, R.id.b2, R.id.bDivide, R.id.b5, R.id.bResult)
    }

    @Test
    fun testOkButton_returnsCorrectResult() {
        launchActivity()
        testHelper(null, R.id.b2, R.id.b5, R.id.bDivide, R.id.b2, R.id.bOK)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val resultData = scenario.result.resultData
        assertThat(resultData).isNotNull()
        val resultAmountStr = resultData.getStringExtra(DatabaseConstants.KEY_AMOUNT)
        assertThat(resultAmountStr).isNotNull()
        // Using BigDecimal for accurate comparison
        assertThat(BigDecimal(resultAmountStr)).isEqualTo(BigDecimal("12.5"))
    }

    @Test
    fun testCancelButton_returnsResultCanceled() {
        launchActivity()
        testHelper(null, R.id.b1, R.id.bCancel)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun testClearButton_resetsDisplayAndOperation() {
        launchActivity()
        testHelper("0", R.id.b1, R.id.b2, R.id.bAdd, R.id.b3, R.id.bClear, expectedOperator = "")
    }

    @Test
    fun testDeleteButton_removesLastDigit() {
        launchActivity()
        testHelper("12", R.id.b1, R.id.b2, R.id.b3, R.id.bDelete)
        testHelper("1", R.id.bDelete)
        testHelper("0", R.id.bDelete)
    }

    @Test
    fun testInitialAmountPassed_displaysCorrectly() {
        val initialAmount = "98.76"
        val intent = Intent(ApplicationProvider.getApplicationContext(), CalculatorInput::class.java)
        intent.putExtra(DatabaseConstants.KEY_AMOUNT, initialAmount)
        launchActivity(intent)
        testHelper("98.76")
    }

    @Test
    fun testPercentButton_calculatesCorrectly() {
        launchActivity()
        testHelper("110", R.id.b1, R.id.b0, R.id.b0, R.id.bAdd, R.id.b1, R.id.b0, R.id.bPercent)  // 100 + 10% (of 100) = 110
        testHelper("90", R.id.b1, R.id.b0, R.id.b0, R.id.bSubtract, R.id.b1, R.id.b0, R.id.bPercent)  // 100 - 10% (of 100) = 90
        testHelper("10", R.id.b1, R.id.b0, R.id.b0, R.id.bMultiply, R.id.b1, R.id.b0, R.id.bPercent)  // 10% (of 100) = 10
        testHelper("1000", R.id.b1, R.id.b0, R.id.b0, R.id.bDivide, R.id.b1, R.id.b0, R.id.bPercent)  // 100 = 10% of 1000
    }

    @Test
    fun testPercentButton_direct_calculatesCorrectly() {
        launchActivity()
        testHelper("0.5", R.id.b5, R.id.b0, R.id.bPercent) // 50% = 0.5
        testHelper("0.005", R.id.bPercent)
    }

    @Test
    fun testPlusMinusButton_negatesDisplay() {
        launchActivity()
        testHelper("-12", R.id.b1, R.id.b2, R.id.bPlusMinus)
        testHelper("12", R.id.bPlusMinus)
    }

    private fun testHelper(expectedResult: String?, vararg  inputs: Int, expectedOperator: String? = null) {
        inputs.forEach {
            onView(withId(it)).perform(click())
        }
        expectedResult?.let {
            onView(withId(R.id.result)).check(matches(withText(expectedResult)))
        }
        expectedOperator?.let {
            onView(withId(R.id.op)).check(matches(withText(expectedOperator)))
        }
    }

    @Test
    fun testStateRestoration_onConfigurationChange_restoresDisplayAndOperator() {
        scenario = ActivityScenario.launch(CalculatorInput::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED) // Ensure activity is fully resumed

        testHelper("3", R.id.b1, R.id.b2, R.id.bAdd, R.id.b3, expectedOperator = "+")

        scenario.recreate()

        // Verify that the state was restored
        // The display should be "3" (last input) and operator should be "+"
        testHelper("3", expectedOperator = "+")
        // Continue interacting to ensure calculator logic is still correct
        testHelper("15", R.id.bResult)
    }

    // TODO: Add more tests:
    // - Division by zero (ensure it handles gracefully, e.g., shows "0" or "Error")
    // - Multiple operations (e.g., 1 + 2 * 3 = 7, not 9 - respecting order or chain) currently not supported
    // - Repeatedly pressing equals
    // - Starting new operation after pressing equals
    // - Inputting very long numbers
    // - Inputting numbers after an operation without pressing equals first
}

