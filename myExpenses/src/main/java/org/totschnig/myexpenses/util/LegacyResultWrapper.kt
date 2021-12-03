package org.totschnig.myexpenses.util

import com.annimon.stream.Exceptional
import com.annimon.stream.Stream
import kotlin.Result

fun <T> Result<T>.asExceptional(): Exceptional<T> = Exceptional.of { getOrThrow() }

fun <T> Exceptional<T>.asResult(): Result<T> = runCatching {
    @Suppress("UsePropertyAccessSyntax")
    getOrThrow()
}

fun <T> Stream<T>.asSequence(): Sequence<T> = Sequence { iterator() }