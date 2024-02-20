package org.totschnig.myexpenses.util

@Suppress("RecursivePropertyAccessor")
val Throwable.safeMessage: String
    get() = message.takeIf { !it.isNullOrBlank() }
        ?: cause?.safeMessage
        ?: "ERROR: ${javaClass.name}"