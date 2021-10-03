package org.totschnig.myexpenses.util

import com.annimon.stream.Exceptional

fun <T> kotlin.Result<T>.asExceptional(): Exceptional<T> = Exceptional.of { getOrThrow() }