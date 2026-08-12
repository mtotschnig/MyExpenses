package org.totschnig.myexpenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.findSiblingParentId
import org.totschnig.myexpenses.db2.insertCurrency
import org.totschnig.myexpenses.db2.loadSplitParts
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.PORTFOLIO_CONTAINER
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import org.totschnig.myexpenses.viewmodel.data.TradeIntent
import org.totschnig.myexpenses.viewmodel.data.TradeType
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TradeTransferTest : BaseViewModelTest() {

    private lateinit var viewModel: MyExpensesV2ViewModel
    private lateinit var portfolioA: FullAccount
    private lateinit var portfolioB: FullAccount
    private val aapl = CurrencyUnit("AAPL", "AAPL", 2, "Apple Inc.", CommodityType.SECURITY)

    @Before
    fun setup() {
        viewModel = MyExpensesV2ViewModel(ApplicationProvider.getApplicationContext(), SavedStateHandle())
        application.appComponent.inject(viewModel)

        runBlocking {
            repository.insertCurrency(aapl.code, aapl.symbol, aapl.description, aapl.fractionDigits, aapl.commodityType)
        }

        val investmentType = repository.findAccountType(AccountType.INVESTMENT.name)!!

        val pA = Account(
            label = "Portfolio A",
            currency = "USD",
            type = investmentType,
            portfolioRole = PORTFOLIO_CONTAINER
        ).createIn(repository)

        val pB = Account(
            label = "Portfolio B",
            currency = "USD",
            type = investmentType,
            portfolioRole = PORTFOLIO_CONTAINER
        ).createIn(repository)

        portfolioA = FullAccount(
            id = pA.id,
            label = pA.label,
            currencyUnit = currencyContext["USD"],
            type = AccountType.INVESTMENT,
            portfolioRole = PORTFOLIO_CONTAINER
        )

        portfolioB = FullAccount(
            id = pB.id,
            label = pB.label,
            currencyUnit = currencyContext["USD"],
            type = AccountType.INVESTMENT,
            portfolioRole = PORTFOLIO_CONTAINER
        )
    }

    @Test
    fun testAssetTransfer() = runTest {
        val intent = TradeIntent(
            targetAsset = aapl,
            type = TradeType.Transfer(false),
            date = LocalDateTime.now(),
            quantity = BigDecimal("10"),
            price = BigDecimal("150"),
            principal = BigDecimal("1500"),
            peerAccountId = portfolioB.id,
            fee = BigDecimal.ZERO
        )

        viewModel.saveTrades(portfolioA, listOf(intent))

        // Verify Portfolio A
        val transactionsA = repository.loadTransactions(portfolioA.id)
        assertThat(transactionsA).hasSize(1)
        val parentA = transactionsA[0]
        assertThat(parentA.categoryId).isEqualTo(SPLIT_CATID)

        val partsA = repository.loadSplitParts(parentA.id)
        assertThat(partsA).hasSize(2)

        val internalLegA = partsA.find { it.transferAccountId != portfolioB.id }!!
        val internalLegAPeer = repository.loadTransaction(internalLegA.transferPeerId!!)
        val externalLegA = partsA.find { it.transferAccountId == portfolioB.id }!!

        assertThat(internalLegA.amount).isEqualTo(150000L) // +Valuation (returning to hub)
        assertThat(internalLegAPeer.data.amount).isEqualTo(-1000L) // -Quantity

        assertThat(externalLegA.amount).isEqualTo(-150000L) // -Valuation (leaving hub)

        // Verify Portfolio B
        val transactionsB = repository.loadTransactions(portfolioB.id)
        assertThat(transactionsB).hasSize(1)
        val parentB = transactionsB[0]

        val partsB = repository.loadSplitParts(parentB.id)
        assertThat(partsB).hasSize(2)

        val externalLegB = partsB.find { it.transferAccountId == portfolioA.id }!!
        val internalLegB = partsB.find { it.transferAccountId != portfolioA.id }!!
        val internalLegBPeer = repository.loadTransaction(internalLegB.transferPeerId!!)

        assertThat(externalLegB.amount).isEqualTo(150000L) // +Valuation (entering hub)
        assertThat(internalLegB.amount).isEqualTo(-150000L) // -Valuation (moving to asset)
        assertThat(internalLegBPeer.data.amount).isEqualTo(1000L) // +Quantity

        // Verify Links
        assertThat(externalLegA.transferPeerId).isEqualTo(externalLegB.id)
        assertThat(externalLegB.transferPeerId).isEqualTo(externalLegA.id)

        // Verify Internal Peers (Spokes)
        assertThat(internalLegA.transferPeerId).isNotNull()
        val peerA = repository.loadTransaction(internalLegA.transferPeerId).data
        assertThat(peerA.accountId).isEqualTo(internalLegA.transferAccountId)
        assertThat(peerA.amount).isEqualTo(-1000L)

        assertThat(internalLegB.transferPeerId).isNotNull()
        val peerB = repository.loadTransaction(internalLegB.transferPeerId).data
        assertThat(peerB.accountId).isEqualTo(internalLegB.transferAccountId)
        assertThat(peerB.amount).isEqualTo(1000L)
    }

    @Test
    fun testIncomingAssetTransfer() = runTest {
        val intent = TradeIntent(
            targetAsset = aapl,
            type = TradeType.Transfer(true),
            date = LocalDateTime.now(),
            quantity = BigDecimal("10"),
            price = BigDecimal("150"),
            principal = BigDecimal("1500"),
            peerAccountId = portfolioA.id,
            fee = BigDecimal.ZERO
        )

        // Recording in Portfolio B, Incoming from Portfolio A
        viewModel.saveTrades(portfolioB, listOf(intent))

        // Verify Portfolio B (recipient where created)
        val transactionsB = repository.loadTransactions(portfolioB.id)
        assertThat(transactionsB).hasSize(1)
        val parentB = transactionsB[0]

        val partsB = repository.loadSplitParts(parentB.id)
        assertThat(partsB).hasSize(2)

        val internalLegB = partsB.find { it.transferAccountId != portfolioA.id }!!
        assertThat(internalLegB.amount).isEqualTo(-150000L) // Moving from Hub to Asset
        val internalLegBPeer = repository.loadTransaction(internalLegB.transferPeerId!!)
        assertThat(internalLegBPeer.data.amount).isEqualTo(1000L) // +Quantity

        // Verify Portfolio A (sender)
        val transactionsA = repository.loadTransactions(portfolioA.id)
        assertThat(transactionsA).hasSize(1)
    }

    @Test
    fun testAssetTransferEdit() = runTest {
        val intent = TradeIntent(
            targetAsset = aapl,
            type = TradeType.Transfer(false),
            date = LocalDateTime.now(),
            quantity = BigDecimal("10"),
            price = BigDecimal("150"),
            principal = BigDecimal("1500"),
            peerAccountId = portfolioB.id,
            fee = BigDecimal.ZERO
        )

        viewModel.saveTrades(portfolioA, listOf(intent))
        val initialId = repository.loadTransactions(portfolioA.id)[0].id
        val initialPeer = repository.findSiblingParentId(initialId)

        val updatedIntent = intent.copy(
            quantity = BigDecimal("20"),
            price = BigDecimal("200"),
            principal = BigDecimal("4000"),
            tradeId = initialId
        )

        viewModel.saveTrade(portfolioA, updatedIntent)

        // Verify Portfolio A
        val transactionsA = repository.loadTransactions(portfolioA.id)
        assertThat(transactionsA).hasSize(1)
        val parentA = transactionsA[0]
        assertThat(parentA.id).isEqualTo(initialId)

        val partsA = repository.loadSplitParts(parentA.id)
        val internalLegA = partsA.find { it.transferAccountId != portfolioB.id }!!
        val internalLegAPeer = repository.loadTransaction(internalLegA.transferPeerId!!)
        assertThat(internalLegA.amount).isEqualTo(400000L)
        assertThat(internalLegAPeer.data.amount).isEqualTo(-2000L)

        // Verify Portfolio B
        val transactionsB = repository.loadTransactions(portfolioB.id)
        assertThat(transactionsB).hasSize(1)
        val parentB = transactionsB[0]
        assertThat(parentB.id).isEqualTo(initialPeer)
        val partsB = repository.loadSplitParts(parentB.id)
        val internalLegB = partsB.find { it.transferAccountId != portfolioA.id }!!
        val internalLegBPeer = repository.loadTransaction(internalLegB.transferPeerId!!)
        assertThat(internalLegB.amount).isEqualTo(-400000L)
        assertThat(internalLegBPeer.data.amount).isEqualTo(2000L)
    }
}
