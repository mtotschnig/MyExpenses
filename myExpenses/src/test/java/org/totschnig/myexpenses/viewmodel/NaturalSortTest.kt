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
        val sorted = list.sortedWith(comparator)
        assertThat(sorted).containsExactly("Item 1", "Item 2", "Item 10").inOrder()
    }

    @Test
    fun testLocaleSensitiveSort() {
        // Test with German locale where 'ä' comes after 'a' but before 'b'
        val comparator = getNaturalComparator(Locale.GERMANY)
        val list = listOf("b", "a", "ä")
        val sorted = list.sortedWith(comparator)
        assertThat(sorted).containsExactly("a", "ä", "b").inOrder()

        // Test with Swedish locale where 'ä' comes after 'z'
        val comparator2 = getNaturalComparator(Locale.forLanguageTag("sv-SE"))
        val list2 = listOf("z", "a", "ä")
        val sorted2 = list2.sortedWith(comparator2)
        assertThat(sorted2).containsExactly("a", "z", "ä").inOrder()
    }

    @Test
    fun testCaseSensitivity() {
        val comparator = getNaturalComparator()
        val list = listOf("B", "a")
        val sorted = list.sortedWith(comparator)
        assertThat(sorted).containsExactly("a", "B").inOrder()
    }
}
