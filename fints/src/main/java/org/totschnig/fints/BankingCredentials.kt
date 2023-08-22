package org.totschnig.fints

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.model2.Bank

@Parcelize
data class BankingCredentials(
    val bankLeitZahl: String,
    val user: String,
    val password: String? = null,
    val bank: Bank? = null
) : Parcelable {
    companion object  {
        val EMPTY = BankingCredentials("", "", null)

        fun fromBank(bank: Bank) = with(bank) {
            BankingCredentials(blz, userId, bank = this)
        }
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