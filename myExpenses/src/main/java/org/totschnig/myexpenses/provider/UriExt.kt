package org.totschnig.myexpenses.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.BuildConfig

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

fun Uri.fromSyncAdapter(): Uri = buildUpon()
    .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_CALLER_IS_SYNCADAPTER, "1")
    .build()

val Uri.isDebugAsset: Boolean
    get() = BuildConfig.DEBUG && scheme == "file" && pathSegments.getOrNull(0) == "android_asset"

fun Uri.withAppendedId(id: Long) = ContentUris.withAppendedId(this, id)