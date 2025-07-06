package org.totschnig.myexpenses.model2

import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IBAN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.util.Utils

data class Party(
    val id: Long = 0L,
    val name: String,
    val shortName: String? = null,
    val iban: String? = null,
    val bic: String? = null,
    val parentId: Long? = null
) {

    val asContentValues
        get() = ContentValues().apply {
            put(KEY_PAYEE_NAME, name.trim())
            put(KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
            put(KEY_SHORT_NAME, shortName?.takeIf { it.isNotEmpty() })
            put(KEY_IBAN, iban)
            put(KEY_BIC, bic)
            put(KEY_PARENTID, parentId)
        }

    companion object {
        const val SELECTION =
            "($KEY_PAYEE_NAME_NORMALIZED LIKE ? OR $KEY_PAYEE_NAME_NORMALIZED GLOB ?)"

        fun selectionArgs(search: String): Array<String> = arrayOf(
            "$search%",
            "*[ (.;,]$search*"
        )

        fun create(
            name: String,
            shortName: String? = null,
            id: Long = 0,
            iban: String? = null,
            bic: String? = null,
            parentId: Long? = null
        ) =
            Party(
                id = id,
                name = requireNotNull(name.trim().takeIf { it.isNotEmpty() }),
                shortName = shortName?.trim(),
                iban = iban,
                bic = bic,
                parentId = parentId
            )
    }
}