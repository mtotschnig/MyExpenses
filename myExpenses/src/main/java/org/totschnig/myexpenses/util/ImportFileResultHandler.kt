package org.totschnig.myexpenses.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.EditText
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.getDisplayName
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.PermissionHelper.canReadUri
import org.totschnig.myexpenses.util.io.FileUtils.getExtension
import androidx.core.net.toUri


object ImportFileResultHandler {
    @Throws(Throwable::class)
    fun handleFilenameRequestResult(hostFragment: FileNameHostFragment, uri: Uri?) {
        val errorMsg: String?
        val context = hostFragment.requireContext()
        val fileNameEditText = hostFragment.filenameEditText
        if (uri != null) {
            fileNameEditText.error = null
            val displayName = context.contentResolver.getDisplayName(uri)
            fileNameEditText.setText(displayName)
            if (canReadUri(uri, context)) {
                val type = context.contentResolver.getType(uri)
                if (type != null) {
                    if (!hostFragment.checkTypeParts(type, getExtension(displayName))) {
                        errorMsg = context.getString(
                            R.string.import_source_select_error,
                            hostFragment.typeName
                        )
                        handleError(errorMsg, fileNameEditText)
                    }
                }
            } else {
                handleError(
                    "Unable to read file. Please select from a different source.",
                    fileNameEditText
                )
            }
        }
    }

    @Throws(Throwable::class)
    private fun handleError(errorMsg: String?, fileNameEditText: EditText) {
        fileNameEditText.error = errorMsg
        throw Throwable(errorMsg)
    }

    fun checkTypePartsDefault(mimeType: String): Boolean {
        val typeParts: Array<String?> =
            mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return typeParts.isNotEmpty() && (typeParts[0] == "*" ||
                typeParts[0] == "text" ||
                typeParts[0] == "application"
                )
    }

    fun maybePersistUri(hostFragment: FileNameHostFragment, prefHandler: PrefHandler) {
        if (!DocumentsContract.isDocumentUri(hostFragment.requireContext(), hostFragment.uri)) {
            prefHandler.putString(hostFragment.prefKey, hostFragment.uri.toString())
        }
    }

    fun handleFileNameHostOnResume(hostFragment: FileNameHostFragment, prefHandler: PrefHandler) {
        if (hostFragment.uri == null) {
            prefHandler.getString(hostFragment.prefKey, "")?.takeIf { it.isNotEmpty() }?.toUri()?.let {
                val context = hostFragment.requireContext()
                if (canReadUri(it, context)) {
                    val displayName = context.contentResolver.getDisplayName(it)
                    hostFragment.uri = it
                    hostFragment.filenameEditText.setText(displayName)
                }
            }
        }
    }

    interface FileNameHostFragment {
        val prefKey: String

        var uri: Uri?

        val filenameEditText: EditText

        fun checkTypeParts(mimeType: String, extension: String): Boolean

        val typeName: String

        fun requireContext(): Context
    }
}
