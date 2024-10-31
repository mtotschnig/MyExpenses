package org.totschnig.myexpenses.testutils

import androidx.test.platform.app.InstrumentationRegistry

val isOrchestrated: Boolean
    get() = !getBooleanInstrumentationArgument("withCleanup")

fun getBooleanInstrumentationArgument(key: String) =
    getInstrumentationArgument(key, "false") == "true"

fun getInstrumentationArgument(key: String, defaultValue: String) =
    InstrumentationRegistry.getArguments().getString(key, defaultValue)

/**
 * With Android Test Orchestrator each test runs on clean database, so cleanup is not necessary,
 * but when we run from commandline with "am instrument" (which is the only way to test the
 * universal apk, e.g. with sqlcrypt module), this is not the case, so we conditionally allow to run
 * cleanup tasks by passing in argument "-e withCleanup true"
 */
fun cleanup(work: () -> Unit) {
    if (!isOrchestrated) {
        work()
    }
}