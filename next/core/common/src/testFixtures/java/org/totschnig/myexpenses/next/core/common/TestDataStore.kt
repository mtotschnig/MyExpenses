package org.totschnig.myexpenses.next.core.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * Runs [block] with a data store in a file of its own, deleted afterwards, like the one the app
 * keeps its decisions in
 */
fun withTestDataStore(block: suspend (DataStore<Preferences>) -> Unit) {
    val directory = createTempDirectory("datastore").toFile()
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    try {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(directory, "test.preferences_pb") }
        runBlocking { block(dataStore) }
    } finally {
        scope.cancel()
        directory.deleteRecursively()
    }
}
