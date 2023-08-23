package org.totschnig.myexpenses.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.floor
import kotlin.math.max

val Bitmap.CompressFormat.asExtension: String
    get() = when(this) {
        Bitmap.CompressFormat.JPEG -> "jpg"
        Bitmap.CompressFormat.PNG -> "png"
        else -> "webp"
    }

//based on https://github.com/rifqimfahmi/BetterImageUpload
object ImageOptimizer {
    fun optimize(
        contentResolver: ContentResolver,
        inputUri: Uri,
        outputUri: Uri,
        compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP,
        maxWidth: Int = 1000,
        maxHeight: Int = 1000,
        quality: Int = 80
    ) {

        val bmOptions: BitmapFactory.Options = decodeBitmapFromUri(contentResolver, inputUri)

        val scaleDownFactor: Float = calculateScaleDownFactor(bmOptions, maxWidth, maxHeight)

        setNearestInSampleSize(bmOptions, scaleDownFactor)

        val matrix: Matrix? = calculateImageMatrix(scaleDownFactor, bmOptions)

        val newBitmap: Bitmap = generateNewBitmap(contentResolver, inputUri, bmOptions, matrix)
            ?: throw IOException("Decoding inputUri failed")

        if (!(contentResolver.openOutputStream(outputUri)?.let {
                compressAndSaveImage(newBitmap, compressFormat, quality, it)
            } ?: throw IOException("Opening output stream failed"))) {
            throw IOException("Compressing bitmap failed")
        }
    }

    private fun decodeBitmapFromUri(
        contentResolver: ContentResolver,
        imageUri: Uri
    ): BitmapFactory.Options {
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val input: InputStream? = contentResolver.openInputStream(imageUri)
        BitmapFactory.decodeStream(input, null, bmOptions)
        input?.close()
        return bmOptions
    }

    private fun calculateScaleDownFactor(
        bmOptions: BitmapFactory.Options,
        maxWidth: Int,
        maxHeight: Int
    ) = max(
        bmOptions.outWidth.toFloat() / maxWidth,
        bmOptions.outHeight.toFloat() / maxHeight
    ).coerceAtLeast(1f)

    private fun setNearestInSampleSize(
        bmOptions: BitmapFactory.Options,
        scaleFactor: Float
    ) {
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = floor(scaleFactor).toInt()
    }

    private fun calculateImageMatrix(
        scaleFactor: Float,
        bmOptions: BitmapFactory.Options
    ): Matrix? {
        val remainingScaleFactor = scaleFactor / bmOptions.inSampleSize.toFloat()
        return if (remainingScaleFactor > 1) {
            Matrix().also {
                it.postScale(1.0f / remainingScaleFactor, 1.0f / remainingScaleFactor)
            }
        } else null
    }

    private fun generateNewBitmap(
        contentResolver: ContentResolver,
        imageUri: Uri,
        bmOptions: BitmapFactory.Options,
        matrix: Matrix?
    ) = contentResolver.openInputStream(imageUri).use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, bmOptions).let {
            if (it != null && matrix != null) {
                val rescaled = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
                it.recycle()
                rescaled
            } else it
        }
    }

    private fun compressAndSaveImage(
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        quality: Int,
        outputStream: OutputStream
    ) = try {
        outputStream.use {
            bitmap.compress(compressFormat, quality, it)
        }
    } finally {
        bitmap.recycle()
    }
}