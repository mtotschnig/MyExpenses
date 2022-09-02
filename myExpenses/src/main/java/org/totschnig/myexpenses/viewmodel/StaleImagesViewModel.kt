package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileCopyUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

class StaleImagesViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    fun saveImages(itemIds: LongArray) {
        viewModelScope.launch(context = coroutineContext()) {
            File(getApplication<MyApplication>().getExternalFilesDir(null), "images.old").also {
                it.mkdir()
            }.takeIf { it.isDirectory }?.let { staleFileDir ->
                itemIds.forEach { id ->
                    val staleImageUri = staleImageUri(id)
                    contentResolver.query(
                            staleImageUri,
                            null,
                            null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val imageFileUri = Uri.parse(cursor.getString(0))
                            val success: Boolean = imageFileUri.lastPathSegment?.takeIf { checkImagePath(it) }?.let { fileName ->
                                if (imageFileUri.scheme == "file") {
                                    imageFileUri.path?.let { File(it) }?.let { file ->
                                        file.renameTo(File(staleFileDir, file.name)).also {
                                            if (it) {
                                                Timber.d("Successfully renamed file %s", imageFileUri.toString())
                                            }
                                        }
                                    } ?: false
                                } else {
                                    try {
                                        FileCopyUtils.copy(imageFileUri, Uri.fromFile(File(staleFileDir, fileName)))
                                        Timber.d("Successfully copied file %s", imageFileUri.toString())
                                        contentResolver.delete(imageFileUri, null, null) > 0
                                    } catch (e: IOException) {
                                        Timber.e(e)
                                        false
                                    }
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
                        if (imageFileUri.lastPathSegment?.let { checkImagePath(it) } == true) {
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
                        } else {
                            Timber.d("%s not deleted since it might still be in use", imageFileUri.toString())
                        }
                        contentResolver.delete(staleImageUri, null, null)
                    }
                }
            }
        }
    }

    private fun staleImageUri(id: Long) = TransactionProvider.STALE_IMAGES_URI.buildUpon().appendPath(id.toString()).build()

    private fun checkImagePath(lastPathSegment: String) = contentResolver.query(
            TransactionProvider.TRANSACTIONS_URI, arrayOf("count(*)"),
            DatabaseConstants.KEY_PICTURE_URI + " LIKE '%" + lastPathSegment + "'", null, null)?.use {
        it.moveToFirst() && it.getInt(0) == 0
    } ?: false
}