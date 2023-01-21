package org.totschnig.myexpenses.model

import android.content.ContentResolver
import org.totschnig.myexpenses.provider.DatabaseConstants

fun planCount(contentResolver: ContentResolver): Int = contentResolver.query(
    Template.CONTENT_URI,
    arrayOf("count(*)"),
    "${DatabaseConstants.KEY_PLANID} is not null",
    null,
    null
)?.use {
    if (it.moveToFirst()) it.getInt(0) else 0
} ?: 0