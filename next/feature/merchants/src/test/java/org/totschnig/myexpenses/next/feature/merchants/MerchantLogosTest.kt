package org.totschnig.myexpenses.next.feature.merchants

import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.totschnig.myexpenses.next.core.common.withTestDataStore
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoConsent
import org.totschnig.myexpenses.next.data.merchants.MerchantLogoStore
import org.totschnig.myexpenses.next.data.merchants.PayeeDomainStore
import org.totschnig.myexpenses.next.data.merchants.PayeeIconStore
import org.totschnig.myexpenses.next.data.merchants.RawPayee

// Without logos found: decoding one needs Android
@OptIn(ExperimentalCoroutinesApi::class)
class MerchantLogosTest {

    private val directory = createTempDirectory("logos").toFile()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        directory.deleteRecursively()
    }

    private fun withLogos(block: suspend (MerchantLogos, PayeeDomainStore, PayeeIconStore) -> Unit) =
        withTestDataStore { dataStore ->
            runTest {
                val domains = PayeeDomainStore(dataStore)
                val icons = PayeeIconStore(dataStore)
                val logos = MerchantLogos(
                    MutableStateFlow(listOf(RawPayee(1, "Stadtwerke", null, null))),
                    MerchantLogoConsent(dataStore),
                    domains,
                    icons,
                    MerchantLogoStore(directory, fetch = { MerchantLogoStore.Fetched.Missing })
                )
                block(logos, domains, icons)
            }
        }

    @Test
    fun showsChosenIconUntilAutomaticAgain() = withLogos { logos, _, icons ->
        logos.setIcon(1, "bolt")
        assertEquals(PayeeImage.Icon("bolt"), logos.images.first { it.isNotEmpty() }[1])
        logos.useAutomatic(1)
        icons.values.first { it.isEmpty() }
        logos.images.first { it.isEmpty() }
    }

    @Test
    fun keepsNoAddressWithoutLogo() = withLogos { logos, domains, _ ->
        val result = CompletableDeferred<Any?>()
        logos.enterDomain(1, "stadtwerke.example") { result.complete(it) }
        assertNull(result.await())
        assertEquals(emptyMap<Long, String>(), domains.domains.first())
    }

    @Test
    fun noDomainIsNoAddress() = withLogos { logos, domains, _ ->
        val result = CompletableDeferred<Any?>()
        logos.enterDomain(1, "no domain") { result.complete(it) }
        assertNull(result.await())
        assertEquals(emptyMap<Long, String>(), domains.domains.first())
    }
}
