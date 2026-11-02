package org.totschnig.myexpenses.next.data.merchants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantsTest {

    @Test
    fun recognizesPayeesAsBanksWriteThem() {
        assertEquals("REWE", Merchants.of("REWE Markt GmbH Fil. 4711")?.name)
        assertEquals("ALDI", Merchants.of("ALDI SUED SAGT DANKE")?.name)
        assertEquals("dm", Merchants.of("DM-Drogerie Markt")?.name)
        assertEquals("dm", Merchants.of("dm fil. 1234 Hamburg")?.name)
        assertEquals("1&1", Merchants.of("1&1 Telecom GmbH")?.name)
        assertEquals("H&M", Merchants.of("H&M 123 Berlin")?.name)
        assertEquals("Apple", Merchants.of("APPLE.COM/BILL")?.name)
    }

    @Test
    fun shortWordsMustBeWholeWords() {
        assertNull(Merchants.of("Dmitri Müller"))
        assertNull(Merchants.of("Obimedia"))
        assertNull(Merchants.of("Ergotherapie Schmidt"))
        assertNull(Merchants.of("Shellfish Bistro"))
    }

    @Test
    fun theMerchantNamedFirstWins() {
        // PayPal takes the money for Netflix
        assertEquals("PayPal", Merchants.of("PayPal Europe S.a.r.l. Netflix")?.name)
        assertEquals("Netflix", Merchants.of("Netflix International B.V. via PayPal")?.name)
    }

    @Test
    fun unknownPayeesHaveNoMerchant() {
        assertNull(Merchants.of("Bäckerei Schulz"))
        assertNull(Merchants.of(null))
    }

    @Test
    fun payeesTakeTheMerchantOfTheirParent() {
        val merchants = Merchants.ofPayees(
            listOf(
                RawPayee(1, "Supermarkt um die Ecke", shortName = "REWE", parentId = null),
                RawPayee(2, "Filiale 4711", shortName = null, parentId = 1),
                RawPayee(3, "Netflix", shortName = null, parentId = 1),
                RawPayee(4, "Bäckerei Schulz", shortName = null, parentId = null),
            )
        )
        assertEquals(mapOf(1L to "REWE", 2L to "REWE", 3L to "Netflix"), merchants.mapValues { it.value.name })
    }

    @Test
    fun savingTipsCheckTheSupermarketsInTheirOrder() {
        assertEquals(
            listOf("REWE", "EDEKA", "ALDI", "Lidl", "Netto", "Penny", "Kaufland", "tegut", "Norma", "Globus", "famila",
                "Alnatura", "denn's", "Tesco", "Sainsbury's", "Asda", "Waitrose", "Walmart", "Kroger"),
            SUPERMARKETS.map { it.first }
        )
        assertEquals(listOf("edeka", "e center", "marktkauf"), SUPERMARKETS[1].second)
    }

    @Test
    fun everyMerchantHasADomainAndAPattern() {
        Merchants.all.forEach {
            assert(it.domain.matches(Regex("[a-z0-9-]+(\\.[a-z0-9-]+)+"))) { it.domain }
            assert(it.patterns.isNotEmpty() || it.words.isNotEmpty()) { it.name }
        }
        assertEquals(Merchants.all.size, Merchants.all.map { it.name }.toSet().size)
    }
}
