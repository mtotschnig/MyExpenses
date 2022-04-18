package org.totschnig.myexpenses.export

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.io.FileUtils
import java.io.IOException

fun createFileFailure(context: Context, parent: DocumentFile, fileName: String) =
    IOException(
        context.getString(
            R.string.io_error_unable_to_create_file,
            fileName, FileUtils.getPath(context, parent.uri)
        )
    )