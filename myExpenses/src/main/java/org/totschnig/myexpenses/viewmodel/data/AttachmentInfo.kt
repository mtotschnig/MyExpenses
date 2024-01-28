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
    val file: File?
) {
    companion object {
        fun of(type: String, thumbnail: Bitmap, file: File?) =
            AttachmentInfo(type, thumbnail, null, null, file)

        fun of(type: String, typeIcon: Icon, file: File?) =
            AttachmentInfo(type, null, typeIcon, null, file)

        fun of(type: String?, fallbackResource: Int, file: File?) =
            AttachmentInfo(type, null, null, fallbackResource, file)
    }
}