package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BankingCredentials(val bankLeitZahl: String, val user: String, val password: String?) : Parcelable {
    companion object  {
        val EMPTY = BankingCredentials("", "", null)
    }
    val isComplete: Boolean
        get() = bankLeitZahl.isNotEmpty() && user.isNotEmpty() && !password.isNullOrEmpty()

    /**
     * [bankLeitZahl] with whitespace removed
     */
    val blz: String
        get() = bankLeitZahl.filter { !it.isWhitespace() }
}