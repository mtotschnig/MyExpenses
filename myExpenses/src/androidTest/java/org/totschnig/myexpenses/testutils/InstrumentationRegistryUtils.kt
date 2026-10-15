package org.totschnig.myexpenses.testutils

import androidx.test.platform.app.InstrumentationRegistry

val isOrchestrated: Boolean
    get() = !getBooleanInstrumentationArgument("withCleanup")

val withEncryption: Boolean
    get() = getBooleanInstrumentationArgument("withEncryption")

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
 * adb install myExpenses/build/outputs/apk_from_bundle/externDebug/myExpenses-extern-debug-universal.apk
 * adb install myExpenses/build/outputs/apk/androidTest/extern/debug/myExpenses-extern-debug-androidTest.apk
 *
 * Run every test class across all subfolders in a separate am instrument call:
 * for testClass in $(find myExpenses/src/androidTest/java/org/totschnig/myexpenses/test -type f \( -name "*Test.kt" -o -name "*Test.java" \) | sed 's|myExpenses/src/androidTest/java/||; s|/|.|g; s|\.kt$||; s|\.java$||'); do \
 *   adb shell am instrument --no-window-animation -w -e clearPackageData true -e withCleanup true -e withEncryption true -e class "$testClass" org.totschnig.myexpenses.debug.test/org.totschnig.myexpenses.MyTestRunner; \
 * done
 */
fun cleanup(work: () -> Unit) {
    if (!isOrchestrated) {
        work()
    }
}

suspend fun cleanupSuspend(work: suspend () -> Unit) {
    if (!isOrchestrated) {
        work()
    }
}