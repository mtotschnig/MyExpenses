package org.totschnig.myexpenses.next.merchants

import org.junit.Assert.assertEquals
import org.junit.Test
import org.totschnig.myexpenses.next.data.merchants.MerchantIcons
import org.totschnig.myexpenses.next.data.merchants.Merchants
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons

/** Checks the icons of the merchants against the font of the app, which only the shell knows */
class MerchantIconsTest {

    @Test
    fun everyMerchantHasAnIconOfTheCatalogOrFontAwesome() {
        assertEquals(
            emptyList<String>(),
            Merchants.all.filter { MerchantIcons.of(it)?.let { icon -> icon in FontAwesomeIcons } != true }.map { it.name }
        )
    }
}
