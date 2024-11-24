package org.totschnig.myexpenses.viewmodel.data

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import java.io.File

data class AttachmentInfo(
    val type: String?,
    val thumbnail: Bitmap?,
    val typeIcon: Icon?,
    @DrawableRes val fallbackResource: Int?,
    val contentDescription: String,
    val file: File?
) {
    companion object {
        fun of(type: String, thumbnail: Bitmap, contentDescription: String, file: File?) =
            AttachmentInfo(type, thumbnail, null, null, contentDescription, file)

        fun of(type: String, typeIcon: Icon, contentDescription: String, file: File?) =
            AttachmentInfo(type, null, typeIcon, null, contentDescription, file)

        fun of(type: String?, fallbackResource: Int, contentDescription: String, file: File?) =
            AttachmentInfo(type, null, null, fallbackResource, contentDescription, file)
    }
}