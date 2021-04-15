package org.totschnig.myexpenses.util

import android.util.SparseBooleanArray
import androidx.core.util.forEach

/**
 * returns a sequence of the keys which are mapped to value true
 */
fun SparseBooleanArray.asTrueSequence() = sequence {
    forEach { key, value -> if (value) yield(key) }
}