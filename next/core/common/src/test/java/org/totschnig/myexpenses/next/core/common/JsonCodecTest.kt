package org.totschnig.myexpenses.next.core.common

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonCodecTest {

    @Test
    fun leavesOutElementsThatCannotBeRead() {
        val json = """[{"name":"a"},{"other":1},{"name":"b"}]"""
        assertEquals(listOf("a", "b"), parseJsonArray(json) { it.getString("name") })
    }

    @Test
    fun leavesOutElementsOfUnknownTypes() {
        val json = """[{"kind":"CONTRACT"},{"kind":"FROM_A_LATER_VERSION"}]"""
        assertEquals(listOf(Kind.CONTRACT), parseJsonArray(json) { Kind.valueOf(it.getString("kind")) })
    }

    @Test
    fun readsBrokenJsonAsEmpty() {
        assertEquals(emptyList<String>(), parseJsonArray("not json") { it.getString("name") })
        assertEquals(emptyList<String>(), parseJsonArray("{}") { it.getString("name") })
    }

    @Test
    fun writesWhatItReads() {
        val names = listOf("a", "b")
        val json = names.toJsonArray { JSONObject().put("name", it) }
        assertEquals(names, parseJsonArray(json) { it.getString("name") })
    }

    @Test
    fun readsSetsOfValues() {
        assertEquals(setOf(1L, 2L), JSONArray(listOf(1, 2, 2)).longs())
        assertEquals(setOf("x", "y"), JSONArray(listOf("x", "y")).strings())
    }

    private enum class Kind { CONTRACT }
}
