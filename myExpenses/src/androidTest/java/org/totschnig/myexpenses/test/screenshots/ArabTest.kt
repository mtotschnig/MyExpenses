package org.totschnig.myexpenses.test.screenshots

import androidx.test.filters.LargeTest
import org.junit.Test
import org.totschnig.myexpenses.testutils.TestMain

//requires shell settings put global hidden_api_policy 1
@LargeTest
class ArabTest: TestMain("ar-SA") {

    @Test
    fun runArab() {
        runScenario("1")
    }
}