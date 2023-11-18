package org.totschnig.myexpenses.test.screenshots

import androidx.test.filters.LargeTest
import org.junit.Test


@LargeTest
class JapaneseTest: TestMain("ja-JP") {

    @Test
    fun runJapanese() {
        runScenario("1")
    }
}