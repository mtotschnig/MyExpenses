package org.totschnig.myexpenses.testutils

import androidx.test.platform.app.InstrumentationRegistry

val isOrchestrated: Boolean
    get() = !getBooleanInstrumentationArgument("withCleanup")

fun getBooleanInstrumentationArgument(key: String) =
    getInstrumentationArgument(key, "false") == "true"

fun getInstrumentationArgument(key: String, defaultValue: String): String =
    InstrumentationRegistry.getArguments().getString(key, defaultValue)

/**
 * With Android Test Orchestrator each test runs on clean database, so cleanup is not necessary,
 * but when we run from commandline with "am instrument" (which is the only way to test the
 * universal apk, e.g. with sqlcrypt module), this is not the case, so we conditionally allow to run
 * cleanup tasks by passing in argument "-e withCleanup true"
 *
 * ./gradlew clean :myExpenses:packageExternDebugUniversalApk assembleExternAndroidTest
 * a install myExpenses/build/outputs/apk_from_bundle/externDebug/myExpenses-extern-debug-universal.apk
 * a install myExpenses/build/outputs/apk/androidTest/extern/debug/myExpenses-extern-debug-androidTest.apk
 * for example run all tests in Espresso package:
 * ~/MyExpenses/myExpenses/src/androidTest/java/org/totschnig/myexpenses/test/espresso$ for i in *; do a shell am instrument --no-window-animation -w -e appendTimestamp false -e class org.totschnig.myexpenses.test.$(basename "$(pwd)").${i%.*} -e clearPackageData true -e withCleanup true org.totschnig.myexpenses.debug.test/org.totschnig.myexpenses.MyTestRunner; done
 */
fun cleanup(work: () -> Unit) {
    if (!isOrchestrated) {
        work()
    }
}