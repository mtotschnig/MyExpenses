package org.totschnig.myexpenses.util

val Throwable.safeMessage: String
    get() = message.takeIf { !it.isNullOrBlank() }
        ?: cause?.safeMessage
        ?: "ERROR: ${javaClass.name}"

/**
 * ClassNotFoundException's message does not explain the exception, just gives the name of the class
 * that is not found. So this message prints the error class name with the error message
 */
val Throwable.safeMessageWithClassName: String
    get() = "${javaClass.simpleName}: ${message.takeIf { !it.isNullOrBlank() } ?: cause?.safeMessage}"
