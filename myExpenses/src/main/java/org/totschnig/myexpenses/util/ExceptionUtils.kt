package org.totschnig.myexpenses.util

val Throwable.safeMessage: String
    get() = message.takeIf { !it.isNullOrBlank() }
        ?: cause?.safeMessage
        ?: "ERROR: ${javaClass.name}"