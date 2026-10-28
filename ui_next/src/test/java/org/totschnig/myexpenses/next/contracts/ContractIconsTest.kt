package org.totschnig.myexpenses.next.contracts

import org.junit.Assert.assertEquals
import org.junit.Test
import org.totschnig.myexpenses.next.data.contracts.BuiltInArea
import org.totschnig.myexpenses.next.data.contracts.icon
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons

class ContractIconsTest {

    @Test
    fun everyIconOfAnAreaExistsInFontAwesome() {
        assertEquals(emptyList<String>(), BuiltInArea.entries.map { it.icon }.filter { it !in FontAwesomeIcons })
    }
}
