package org.totschnig.myexpenses.util

import android.util.SparseBooleanArray
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollectionUtilsRobolectricTest {

    @Test
    fun testAsTrueSequence() {
        Assertions.assertThat(SparseBooleanArray().apply {
            put(1, true)
            put(2, false)
            put(3, true)
        }.asTrueSequence().toList()).isEqualTo(listOf(1, 3))
    }
}