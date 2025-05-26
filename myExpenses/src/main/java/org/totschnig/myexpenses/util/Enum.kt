package org.totschnig.myexpenses.util

inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T =
    enumValueOrNull<T>(name) ?: default

inline fun <reified T : Enum<T>> enumValueOrNull(name: String?): T? =
    name?.let {
        try {
            enumValueOf<T>(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    }