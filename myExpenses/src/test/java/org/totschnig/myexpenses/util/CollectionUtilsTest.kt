package org.totschnig.myexpenses.util

import com.google.common.truth.Truth.assertThat
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class CollectionUtilsTest {

    @Test
    @Parameters(method = "parametersToTestReplace")
    fun testReplace(input: Set<Long>, replace: Iterable<Long>, new: Long, expected: Set<Long>) {
        assertThat(input.replace(replace, new)).isEqualTo(expected)
    }

    @Suppress("unused")
    private fun parametersToTestReplace(): Array<Any> {
        return arrayOf(
                arrayOf(setOf<Long>(1, 2, 3), listOf<Long>(2), 3, setOf<Long>(1, 3)),
                arrayOf(setOf<Long>(1, 2, 3), listOf<Long>(4), 5, setOf<Long>(1, 2, 3)),
                arrayOf(setOf<Long>(1, 2, 3), listOf<Long>(1 , 2), 5, setOf<Long>(3, 5)),
                arrayOf(setOf<Long>(3, 5), listOf<Long>(5), 1, setOf<Long>(1, 3))
        )
    }
}