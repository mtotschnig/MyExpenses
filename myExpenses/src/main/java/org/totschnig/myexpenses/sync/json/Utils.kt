package org.totschnig.myexpenses.sync.json

import kotlinx.serialization.json.Json
import java.io.InputStream

object Utils {
    fun getChanges(inputStream: InputStream) =
        getChanges(inputStream.bufferedReader().use { it.readText() })

    fun getChanges(jsonString: String) = Json.decodeFromString<List<TransactionChange>>(jsonString)
}
