package org.totschnig.myexpenses.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri

@SuppressLint("InlinedApi")
fun Uri.withLimit(limit: Int, offset: Int? = null): Uri = buildUpon()
    .appendQueryParameter(
        ContentResolver.QUERY_ARG_LIMIT,
        limit.toString()
    ).apply {
        offset?.let {
            appendQueryParameter(
                ContentResolver.QUERY_ARG_OFFSET,
                it.toString()
            )
        }
    }.build()