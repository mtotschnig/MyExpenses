package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bank(val name: String, val user: String, val password: String?) : Parcelable {
    companion object  {
        val EMPTY = Bank("", "", null)
    }
}