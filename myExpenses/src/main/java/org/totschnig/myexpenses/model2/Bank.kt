package org.totschnig.myexpenses.model2

data class Bank(
    val id: Long = 0L,
    val blz: String,
    val bic: String,
    val bankName: String,
    val userId: String,
    /**
     * holds the number of accounts linked to this bank
     */
    val count: Int = 0
) {
    override fun toString(): String {
        return "$bankName ($userId)"
    }
}