package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bank(val bankLeitZahl: String, val user: String, val password: String?) : Parcelable {
    companion object  {
        val EMPTY = Bank("", "", null)
    }
    val isComplete: Boolean
        get() = bankLeitZahl.isNotEmpty() && user.isNotEmpty() && !password.isNullOrEmpty()
}