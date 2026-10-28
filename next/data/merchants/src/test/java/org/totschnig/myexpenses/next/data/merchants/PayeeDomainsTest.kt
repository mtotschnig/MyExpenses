package org.totschnig.myexpenses.next.data.merchants

import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.totschnig.myexpenses.next.core.common.withTestDataStore

class PayeeDomainsTest {

    @Test
    fun takesTheDomainOfAWebAddressAsTyped() {
        assertEquals("rewe.de", domainOf("rewe.de"))
        assertEquals("rewe.de", domainOf(" https://www.REWE.de/angebote?x=1#top "))
        assertEquals("shop.example.com", domainOf("http://user@shop.example.com:8080/"))
        assertEquals("xn--bckerei-schulz-5hb.de", domainOf("bäckerei-schulz.de"))
    }

    @Test
    fun rejectsWhatIsNoDomain() {
        assertNull(domainOf(""))
        assertNull(domainOf("rewe"))
        assertNull(domainOf("Bäckerei Schulz"))
        assertNull(domainOf("https://"))
    }

    private val payees = listOf(
        RawPayee(1, "REWE Markt GmbH", shortName = null, parentId = null),
        RawPayee(2, "Filiale 4711", shortName = null, parentId = 1),
        RawPayee(3, "Bäckerei Schulz", shortName = null, parentId = null),
        RawPayee(4, "Schulz Filiale Süd", shortName = null, parentId = 3),
    )

    @Test
    fun theDomainEnteredComesFirstThenThatOfTheParentThenTheMerchant() {
        assertEquals(
            mapOf(1L to "rewe.de", 2L to "rewe.de", 3L to "baecker-schulz.de", 4L to "baecker-schulz.de"),
            payeeDomains(payees, mapOf(3L to "baecker-schulz.de"), withCatalog = true)
        )
        assertEquals(
            mapOf(1L to "rewe.de", 2L to "rewe-filiale.de", 3L to "baecker-schulz.de", 4L to "baecker-schulz.de"),
            payeeDomains(payees, mapOf(3L to "baecker-schulz.de", 2L to "rewe-filiale.de"), withCatalog = true)
        )
    }

    @Test
    fun withoutConsentOnlyTheDomainsEnteredCount() {
        assertEquals(
            mapOf(3L to "baecker-schulz.de", 4L to "baecker-schulz.de"),
            payeeDomains(payees, mapOf(3L to "baecker-schulz.de"), withCatalog = false)
        )
    }

    @Test
    fun anIconChosenComesFirstAndAlsoCountsForThePayeesBelow() {
        assertEquals(
            mapOf(
                1L to PayeeLook.Domain("rewe.de"),
                2L to PayeeLook.Icon("cart-shopping"),
                3L to PayeeLook.Icon("bread-slice"),
                4L to PayeeLook.Domain("schulz-sued.de"),
            ),
            payeeLooks(
                payees,
                entered = mapOf(3L to "baecker-schulz.de", 4L to "schulz-sued.de"),
                icons = mapOf(2L to "cart-shopping", 3L to "bread-slice"),
                withCatalog = true
            )
        )
    }

    @Test
    fun withoutConsentMerchantsShowAnIcon() {
        assertEquals(
            mapOf(
                1L to PayeeLook.Icon("cart-shopping"),
                2L to PayeeLook.Icon("cart-shopping"),
                3L to PayeeLook.Icon("bread-slice"),
                4L to PayeeLook.Icon("bread-slice")
            ),
            payeeLooks(payees, entered = emptyMap(), icons = mapOf(3L to "bread-slice"), withCatalog = false)
        )
    }

    @Test
    fun storesIconsByPayee() = withTestDataStore { dataStore ->
        val store = PayeeIconStore(dataStore)
        store.set(3, "bread-slice")
        assertEquals(mapOf(3L to "bread-slice"), store.values.first())
        assertEquals(emptyMap<Long, String>(), PayeeDomainStore(dataStore).domains.first())
    }

    @Test
    fun storesDomainsByPayee() = withTestDataStore { dataStore ->
        val store = PayeeDomainStore(dataStore)
        assertEquals(emptyMap<Long, String>(), store.domains.first())
        store.set(3, "baecker-schulz.de")
        store.set(7, "example.com")
        store.set(7, null)
        assertEquals(mapOf(3L to "baecker-schulz.de"), store.domains.first())
    }
}
