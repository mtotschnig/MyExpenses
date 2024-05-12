package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.TextUtils
import androidx.core.text.color
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.safeMessage
import java.io.File
import java.io.IOException

class StaleImagesViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    private val _result: MutableStateFlow<CharSequence?> = MutableStateFlow(null)
    val result: StateFlow<CharSequence?> = _result

    private fun buildArchiveUri(fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .takeIf { it.exists() || it.mkdir() }
                ?.let { File(it, "MyExpenses.Attachments.Archive") }
                ?.takeIf { it.exists() || it.mkdir() }
                ?.let { Uri.fromFile(File(it, fileName)) }
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Documents/MyExpenses.Attachments.Archive"
                )
            }
            contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        }
    }

    private fun appendSuccess(result: String) {
        appendSpacerIfNeeded()
        postUpdate(result)
    }

    private fun onException(exception: Exception) {
        appendSpacerIfNeeded()
        postUpdate(SpannableStringBuilder().color(Color.RED) {
            append(exception.safeMessage)
        })
        CrashHandler.report(exception)
    }

    private fun appendSpacerIfNeeded() {
        if (_result.value?.isNotEmpty() == true) postUpdate(" ")
    }

    private fun postUpdate(result: CharSequence) {
        _result.update {
            _result.value?.let {
                TextUtils.concat(it, result)
            } ?: result
        }
    }

    fun saveImages(itemIds: LongArray) {
        viewModelScope.launch(context = coroutineContext()) {
            itemIds.forEach { id ->
                val staleImageUri = staleImageUri(id)
                contentResolver.query(
                    staleImageUri,
                    null,
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val imageFileUri = Uri.parse(cursor.getString(0))
                        val success: Boolean = imageFileUri.lastPathSegment?.let { fileName ->
                            try {
                                val mimeType = contentResolver.getType(imageFileUri)
                                    ?: throw IOException("Could not get MIME type")
                                val archiveUri = buildArchiveUri(fileName, mimeType)
                                    ?: throw IOException("Could not get output URI")
                                FileCopyUtils.copy(contentResolver, imageFileUri, archiveUri)
                                appendSuccess("Successfully copied file $imageFileUri to $archiveUri. ")
                                if (contentResolver.delete(imageFileUri, null, null) == 0)
                                    throw IOException("") else true
                            } catch (e: Exception) {
                                onException(e)
                                false
                            }
                        } ?: run {
                            appendSuccess("$imageFileUri not moved since image might still be in use. ")
                            true //we do not move the file but remove its uri from the table
                        }
                        if (success) {
                            contentResolver.delete(staleImageUri, null, null)
                        }
                    }
                }
            }
        }
    }

    fun deleteImages(itemIds: LongArray) {
        viewModelScope.launch(context = coroutineContext()) {
            itemIds.forEach { id ->
                val staleImageUri = staleImageUri(id)
                contentResolver.query(
                    staleImageUri,
                    null,
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val imageFileUri = Uri.parse(cursor.getString(0))
                        val success = if (imageFileUri.scheme == "file") {
                            imageFileUri.path?.let { File(it).delete() } ?: false
                        } else {
                            contentResolver.delete(imageFileUri, null, null) > 0
                        }
                        if (success) {
                            appendSuccess("Successfully deleted file $imageFileUri")
                        } else {
                            onException(Exception("Unable to delete file $imageFileUri"))
                        }
                        contentResolver.delete(staleImageUri, null, null)
                    }
                }
            }
        }
    }

    private fun staleImageUri(id: Long) =
        TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(id.toString()).build()
}