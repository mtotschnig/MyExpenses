package org.totschnig.myexpenses.util.io

import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile

val DocumentFile.displayName: String
    @WorkerThread get() = if (uri.scheme == "file") {
        uri.path
    } else {
        val name = name
        val authority = uri.authority
        if (name != null && authority != null) {
            "$name ($authority)"
        } else {
           null
        }
    } ?: uri.toString()