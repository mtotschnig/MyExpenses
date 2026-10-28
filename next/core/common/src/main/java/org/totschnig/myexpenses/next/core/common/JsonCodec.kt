package org.totschnig.myexpenses.next.core.common

import java.time.DateTimeException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads what the new UI stores as JSON array in the data store. Elements that cannot be read, e.g.
 * of a type from a later version, are left out; JSON that cannot be read gives an empty list.
 */
fun <T> parseJsonArray(json: String, element: (JSONObject) -> T): List<T> = try {
    JSONArray(json).let { array ->
        (0 until array.length()).mapNotNull { i ->
            try {
                element(array.getJSONObject(i))
            } catch (_: JSONException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: DateTimeException) {
                null
            }
        }
    }
} catch (_: JSONException) {
    emptyList()
}

fun <T> List<T>.toJsonArray(element: (T) -> JSONObject): String = JSONArray(map(element)).toString()

/** The values of a JSON array of numbers */
fun JSONArray.longs(): Set<Long> = (0 until length()).mapTo(HashSet()) { getLong(it) }

fun JSONArray.strings(): Set<String> = (0 until length()).mapTo(HashSet()) { getString(it) }
