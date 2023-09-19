package org.totschnig.myexpenses.viewmodel.data

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes

data class AttachmentInfo(
    val thumbnail: Bitmap?,
    val typeIcon: Icon?,
    @DrawableRes val fallbackResource: Int?
) {
    companion object {
        fun of(thumbnail: Bitmap) = AttachmentInfo(thumbnail, null, null)
        fun of(typeIcon: Icon) = AttachmentInfo(null, typeIcon, null)
        fun of(fallbackResource: Int) = AttachmentInfo(null, null, fallbackResource)
    }
}