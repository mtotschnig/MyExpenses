package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(val id: Long, val label: String, var selected: Boolean, val count: Int = 0) :
    Parcelable {
    init {
        check(id > 0)
    }

    override fun toString() = label
}