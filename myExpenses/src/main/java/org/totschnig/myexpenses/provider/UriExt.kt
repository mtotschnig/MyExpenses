package org.totschnig.myexpenses.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile

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

fun Uri.fileName(context: Context) = DocumentFile.fromSingleUri(context, this)!!.name ?: lastPathSegment!!