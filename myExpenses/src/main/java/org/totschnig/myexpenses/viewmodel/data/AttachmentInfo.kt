package org.totschnig.myexpenses.viewmodel.data

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import java.io.File

data class AttachmentInfo(
    val thumbnail: Bitmap?,
    val typeIcon: Icon?,
    @DrawableRes val fallbackResource: Int?,
    val file: File?
) {
    companion object {
        fun of(thumbnail: Bitmap, file: File?) = AttachmentInfo(thumbnail, null, null, file)
        fun of(typeIcon: Icon, file: File?) = AttachmentInfo(null, typeIcon, null, file)
        fun of(fallbackResource: Int, file: File?) = AttachmentInfo(null, null, fallbackResource, file)
    }
}