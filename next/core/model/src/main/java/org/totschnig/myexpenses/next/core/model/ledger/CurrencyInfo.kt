package org.totschnig.myexpenses.next.core.model.ledger

/**
 * A currency as the new UI needs it to show amounts, which are stored in minor units
 *
 * @param fractionDigits number of minor units, e.g. 2 for cents
 */
data class CurrencyInfo(val code: String, val symbol: String, val fractionDigits: Int) {
    companion object {
        /** For previews and tests */
        val Preview = CurrencyInfo("EUR", "€", 2)
    }
}
