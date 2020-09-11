package org.totschnig.ocr

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface

fun getImageRotation(contentResolver: ContentResolver, imageUri: Uri) = when(imageUri.scheme) {
    "content" -> getOrientationFromMediaStore(contentResolver, imageUri)
    "file" -> exifInDegrees(ExifInterface(imageUri.path!!))
    else -> 0
}

@SuppressLint("Recycle")
private fun getOrientationFromMediaStore(contentResolver: ContentResolver, imageUri: Uri): Int {
    var orientation = -1
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val projection = arrayOf(MediaStore.Images.ImageColumns.ORIENTATION)
        contentResolver.query(imageUri, projection, null, null, null)?.let {
            if (it.moveToFirst()) {
                orientation = it.getInt(0)
            }
            it.close()
        }
    } else {
        contentResolver.openInputStream(imageUri)?.use { inputStream ->
            orientation = exifInDegrees(ExifInterface(inputStream))
        }
    }
    return orientation
}

private fun exifInDegrees(exifInterface: ExifInterface) =
        when(exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }