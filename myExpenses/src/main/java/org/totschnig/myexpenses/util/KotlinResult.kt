package org.totschnig.myexpenses.util

import android.content.Context
import androidx.annotation.StringRes
import kotlin.Result

fun <T> Result.Companion.failure(
    context: Context,
    @StringRes resId: Int,
    vararg formatArgs: Any?
): Result<T> = failure(localizedThrowable(context, resId, *formatArgs))

val ResultUnit = Result.success(Unit)

fun localizedThrowable(
    context: Context,
    @StringRes resId: Int,
    vararg formatArgs: Any?
) = Throwable(context.getString(resId, *formatArgs))