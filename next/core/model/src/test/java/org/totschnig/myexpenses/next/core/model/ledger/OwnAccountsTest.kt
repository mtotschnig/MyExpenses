package org.totschnig.myexpenses.next.core.model.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnAccountsTest {

    private val giro = 1L
    private val savings = 3L
    private val ownIbans = mapOf("DE89370400440532013000" to savings, "DE02120300000000202051" to giro)

    @Test
    fun recognizesOwnTransfers() {
        // Transfer in the app
        assertEquals(savings, ownTransferTarget(giro, savings, null, ownIbans))
        // Imported from the bank, IBAN written with spaces and in lower case
        assertEquals(savings, ownTransferTarget(giro, null, "de89 3704 0044 0532 0130 00", ownIbans))
        // Payment to someone else
        assertNull(ownTransferTarget(giro, null, "DE75512108001245126199", ownIbans))
        assertNull(ownTransferTarget(giro, null, null, ownIbans))
        // The own account itself is no target
        assertNull(ownTransferTarget(giro, null, "DE02120300000000202051", ownIbans))
    }
}
