package org.totschnig.myexpenses.next.data.merchants

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoStore.Fetched

class MerchantLogoStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val icon = byteArrayOf(1, 2, 3)
    private val prepared = byteArrayOf(9)
    private var answer: Fetched = Fetched.Icon(icon)
    private val asked = mutableListOf<String>()
    private var time = 1_000_000_000_000L

    private fun store() = MerchantLogoStore(
        dir = folder.root.resolve("logos"),
        fetch = { asked += it; answer },
        prepare = { if (it.contentEquals(icon)) prepared else null },
        now = { time }
    )

    @Test
    fun loadsALogoOnceAndKeepsIt() = runTest {
        assertArrayEquals(prepared, store().logo("rewe.de"))
        // Also after a restart
        assertArrayEquals(prepared, store().logo("rewe.de"))
        assertEquals(listOf("rewe.de"), asked)
    }

    @Test
    fun remembersDomainsWithoutLogoForAWhile() = runTest {
        answer = Fetched.Missing
        assertNull(store().logo("lidl.de"))
        time += MerchantLogoStore.RETRY_AFTER_MS - 1
        assertNull(store().logo("lidl.de"))
        assertEquals(1, asked.size)
        time += 2
        answer = Fetched.Icon(icon)
        assertArrayEquals(prepared, store().logo("lidl.de"))
        assertEquals(2, asked.size)
    }

    @Test
    fun anUnusableIconCountsAsMissing() = runTest {
        answer = Fetched.Icon(byteArrayOf(0))
        assertNull(store().logo("kaufland.de"))
        assertNull(store().logo("kaufland.de"))
        assertEquals(1, asked.size)
    }

    @Test
    fun asksAgainAfterAFailure() = runTest {
        answer = Fetched.Failed
        assertNull(store().logo("rewe.de"))
        answer = Fetched.Icon(icon)
        assertArrayEquals(prepared, store().logo("rewe.de"))
        assertEquals(2, asked.size)
    }

    @Test
    fun clearRemovesTheLogosExceptThoseToKeep() = runTest {
        val store = store()
        store.logo("rewe.de")
        store.logo("baecker-schulz.de")
        store.clear(keep = setOf("baecker-schulz.de"))
        assertFalse(folder.root.resolve("logos/rewe.de.png").exists())
        assertArrayEquals(prepared, store.logo("baecker-schulz.de"))
        assertArrayEquals(prepared, store.logo("rewe.de"))
        assertEquals(listOf("rewe.de", "baecker-schulz.de", "rewe.de"), asked)
    }

    @Test
    fun asksAgainWhenTheUserAsks() = runTest {
        answer = Fetched.Missing
        assertNull(store().logo("lidl.de"))
        answer = Fetched.Icon(icon)
        assertNull(store().logo("lidl.de"))
        assertArrayEquals(prepared, store().logo("lidl.de", retry = true))
        assertEquals(2, asked.size)
    }
}
