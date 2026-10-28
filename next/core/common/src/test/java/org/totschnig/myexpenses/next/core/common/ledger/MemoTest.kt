package org.totschnig.myexpenses.next.core.common.ledger

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MemoTest {

    private data class Input(val values: List<Int>)

    @Test
    fun reusesTheResultForAnEqualInput() {
        val memo = Memo<Input, List<Int>>("test") { input -> input.values.map { it * 2 } }
        val first = memo.of(Input(listOf(1, 2)))
        // An equal input read again, e.g. after the app was in the background
        assertSame(first, memo.of(Input(listOf(1, 2))))
        assertEquals(1, memo.computations)
        assertEquals(listOf(2, 6), memo.of(Input(listOf(1, 3))))
        assertEquals(2, memo.computations)
    }

    @Test
    fun computesOnlyNewInputsAndKeepsNull() = runTest {
        val memo = Memo<Input, Int>("test") { it.values.sum() }
        val results = flowOf(null, Input(listOf(1)), Input(listOf(1)), Input(listOf(2)), null)
            .mapMemo(memo)
            .toList()
        assertEquals(listOf(null, 1, 2, null), results)
        assertEquals(2, memo.computations)
    }
}
