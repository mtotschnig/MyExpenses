package org.totschnig.myexpenses.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class NaturalSortTest {

    @Test
    fun testNaturalOrder() {
        val comparator = getNaturalComparator()
        val list = listOf("Item 10", "Item 2", "Item 1")
        assertThat(list.sortedWith(comparator)).containsExactly("Item 1", "Item 2", "Item 10").inOrder()
    }

    @Test
    fun testLocaleSensitiveSort() {
        // Test with German locale where 'ä' comes after 'a' but before 'b'
        val comparator = getNaturalComparator(Locale.GERMANY)
        val list = listOf("b", "a", "ä")
        assertThat(list.sortedWith(comparator)).containsExactly("a", "ä", "b").inOrder()

        // Test with Swedish locale where 'ä' comes after 'z'
        val comparator2 = getNaturalComparator(Locale.forLanguageTag("sv-SE"))
        val list2 = listOf("z", "a", "ä")
        assertThat(list2.sortedWith(comparator2)).containsExactly("a", "z", "ä").inOrder()
    }

    @Test
    fun testCaseSensitivity() {
        val comparator = getNaturalComparator()
        val list = listOf("B", "a")
        assertThat(list.sortedWith(comparator)).containsExactly("a", "B").inOrder()
    }

    @Test
    fun testSafeSortedWithSortsCorrectly() {
        val comparator = getNaturalComparator()
        val list = listOf("Item 10", "Item 2", "Item 1")
        assertThat(list.safeSortedWith(comparator)).containsExactly("Item 1", "Item 2", "Item 10").inOrder()
    }

    @Test
    fun testSafeSortedWithFallsBackOnException() {
        val throwingComparator = Comparator<String> { _, _ -> throw RuntimeException("simulated ICU failure") }
        val list = listOf("b", "a", "c")
        assertThat(list.safeSortedWith(throwingComparator)).containsExactlyElementsIn(list).inOrder()
    }

    @Test
    fun testSafeSortedWithRethrowsCancellation() {
        val cancellationComparator = Comparator<String> { _, _ ->
            throw kotlinx.coroutines.CancellationException("cancelled")
        }
        val list = listOf("b", "a")
        try {
            list.safeSortedWith(cancellationComparator)
            error("Expected CancellationException to be rethrown")
        } catch (e: kotlinx.coroutines.CancellationException) {
            // expected
        }
    }
}
