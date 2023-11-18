package org.totschnig.myexpenses.test.screenshots

import androidx.test.filters.LargeTest
import org.junit.Test

@LargeTest
class ArabTest: TestMain("ar-SA") {

    @Test
    fun runArab() {
        runScenario("1")
    }
}