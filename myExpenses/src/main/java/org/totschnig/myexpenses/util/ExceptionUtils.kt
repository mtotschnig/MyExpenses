package org.totschnig.myexpenses.util

val Throwable.safeMessage: String
    get() = message?: "ERROR: ${javaClass.name}"