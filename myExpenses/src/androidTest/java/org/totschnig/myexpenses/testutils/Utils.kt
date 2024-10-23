package org.totschnig.myexpenses.testutils

import androidx.test.platform.app.InstrumentationRegistry

fun getBooleanInstrumentationArgument(key: String) =
    getInstrumentationArgument(key, "false") == "true"

fun getInstrumentationArgument(key: String, defaultValue: String) =
    InstrumentationRegistry.getArguments().getString(key, defaultValue)