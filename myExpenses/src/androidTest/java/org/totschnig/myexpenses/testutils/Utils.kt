package org.totschnig.myexpenses.testutils

import androidx.test.platform.app.InstrumentationRegistry

val isOrchestrated: Boolean
    get() = false //TODO pass in argument

fun getBooleanInstrumentationArgument(key: String) =
    getInstrumentationArgument(key, "false") == "true"

fun getInstrumentationArgument(key: String, defaultValue: String) =
    InstrumentationRegistry.getArguments().getString(key, defaultValue)