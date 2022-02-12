package org.totschnig.myexpenses.util

import com.annimon.stream.Exceptional
import kotlin.Result

fun <T> Result<T>.asExceptional(): Exceptional<T> = Exceptional.of { getOrThrow() }
fun <T> List<Result<T>>.asExceptional(): List<Exceptional<T>> = map { Exceptional.of { it.getOrThrow() } }
