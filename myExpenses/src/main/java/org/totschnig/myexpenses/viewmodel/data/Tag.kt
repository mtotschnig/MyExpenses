package org.totschnig.myexpenses.viewmodel.data

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(val id: Long, val label: String, @ColorInt val color: Int? = Color.BLACK, val count: Int = 0) :
    Parcelable {
    init {
        check(id > 0)
    }

    override fun toString() = label
}