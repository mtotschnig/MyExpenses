package org.totschnig.myexpenses.util

import com.annimon.stream.Exceptional
import com.annimon.stream.Stream

fun <T> kotlin.Result<T>.asExceptional(): Exceptional<T> = Exceptional.of { getOrThrow() }

fun <T> Stream<T>.asSequence(): Sequence<T> = Sequence { iterator() }