package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BankingCredentials(
    val bankLeitZahl: String,
    val user: String,
    val password: String? = null,
    val bank: Pair<Long, String>? = null
) : Parcelable {
    companion object  {
        val EMPTY = BankingCredentials("", "", null)
    }
    val isComplete: Boolean
        get() = bankLeitZahl.isNotEmpty() && user.isNotEmpty() && !password.isNullOrEmpty()

    val isNew: Boolean
        get() = bank == null

    /**
     * [bankLeitZahl] with whitespace removed
     */
    val blz: String
        get() = bankLeitZahl.filter { !it.isWhitespace() }
}