package org.totschnig.myexpenses.model2

data class Party(
    val id: Long = 0L,
    val name: String,
    val iban: String? = null,
    val bic: String? = null
) {
    companion object {
        fun create(name: String, iban: String?, bic: String?) =
            Party(name = name.trim(), iban = iban, bic = bic)
    }
}