package org.totschnig.myexpenses.util

fun joinArrays(a1: Array<String>?, a2: Array<String>?) = when {
    a1 == null || a1.isEmpty() -> {
        a2
    }
    a2 == null || a2.isEmpty() -> {
        a1
    }
    else -> a1 + a2
}