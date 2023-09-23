package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileCopyUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

class StaleImagesViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    private fun buildArchiveUri(fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT < 29) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .takeIf { it.exists() || it.mkdir() }
                ?.let { File(it, "MyExpenses.Attachments.Archive") }
                ?.takeIf { it.exists() || it.mkdir() }
                ?.let { Uri.fromFile(File(it, fileName)) }
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/MyExpenses.Attachments.Archive")
            }
            contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        }
    }

    fun saveImages(itemIds: LongArray) {
        viewModelScope.launch(context = coroutineContext()) {
            itemIds.forEach { id ->
                val staleImageUri = staleImageUri(id)
                contentResolver.query(
                    staleImageUri,
                    null,
                    null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val imageFileUri = Uri.parse(cursor.getString(0))
                        val success: Boolean = imageFileUri.lastPathSegment?.let { fileName ->
                            try {
                                val mimeType = contentResolver.getType(imageFileUri) ?: throw IOException("Could not get MIME type")
                                val archiveUri = buildArchiveUri(fileName, mimeType) ?: throw IOException("Could not get output URI")
                                FileCopyUtils.copy(contentResolver, imageFileUri, archiveUri)
                                Timber.d("Successfully copied file %s", imageFileUri.toString())
                                contentResolver.delete(imageFileUri, null, null) > 0
                            } catch (e: IOException) {
                                Timber.e(e)
                                false
                            }
                        } ?: run {
                            Timber.d("%s not moved since image might still be in use", imageFileUri.toString())
                            true //we do not move the file but remove its uri from the table
                        }
                        if (success) {
                            contentResolver.delete(staleImageUri, null, null)
                        } else {
                            CrashHandler.report(Exception("Unable to move file $imageFileUri"))
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
                        null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val imageFileUri = Uri.parse(cursor.getString(0))
                        val success = if (imageFileUri.scheme == "file") {
                            imageFileUri.path?.let { File(it).delete() } ?: false
                        } else {
                            contentResolver.delete(imageFileUri, null, null) > 0
                        }
                        if (success) {
                            Timber.d("Successfully deleted file %s", imageFileUri.toString())
                        } else {
                            CrashHandler.report(Exception("Unable to delete file $imageFileUri"))
                        }
                        contentResolver.delete(staleImageUri, null, null)
                    }
                }
            }
        }
    }

    private fun staleImageUri(id: Long) = TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(id.toString()).build()
}