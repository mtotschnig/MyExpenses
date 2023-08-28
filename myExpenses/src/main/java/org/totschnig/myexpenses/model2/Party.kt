package org.totschnig.myexpenses.model2

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.util.Utils

data class Party(
    val id: Long = 0L,
    val name: String,
    val shortName: String? = null,
    val iban: String? = null,
    val bic: String? = null
) {

    val asContentValues
        get() = ContentValues().apply {
            put(KEY_PAYEE_NAME, name)
            put(KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
            put(KEY_SHORT_NAME, shortName)
            put(KEY_IBAN, iban)
            put(KEY_BIC, bic)
        }

    companion object {
        fun create(name: String, iban: String?, bic: String?) =
            Party(name = name.trim(), iban = iban, bic = bic)
    }
}