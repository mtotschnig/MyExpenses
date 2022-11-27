package org.totschnig.myexpenses.util.io

import androidx.documentfile.provider.DocumentFile

val DocumentFile.displayName
    get() = (
            if (uri.scheme == "file")
                uri.path
            else
                "${name}${uri.authority?.let { " ($it)" } ?: ""}"
            ) ?: uri.toString()