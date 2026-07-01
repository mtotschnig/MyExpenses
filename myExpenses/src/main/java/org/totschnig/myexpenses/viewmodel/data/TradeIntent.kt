package org.totschnig.myexpenses.viewmodel.data

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal
import java.time.LocalDateTime

sealed class TradeType(@param:StringRes val label: Int) {
    sealed class AssetTrade(@param:StringRes label: Int) : TradeType(label) {
        data object BUY : AssetTrade(R.string.trade_buy)
        data object SELL : AssetTrade(R.string.trade_sell)
    }

    sealed class CashMovement(@param:StringRes label: Int) : TradeType(label) {
        data object DEPOSIT : CashMovement(R.string.trade_deposit)
        data object WITHDRAW : CashMovement(R.string.trade_withdraw)
    }

    companion object {
        val entries = listOf(AssetTrade.BUY, AssetTrade.SELL, CashMovement.DEPOSIT, CashMovement.WITHDRAW)
    }
}

enum class FundingSource {
    PORTFOLIO, // Uses the portfolio's cash balance
    EXTERNAL,  // Not tracked in the app (no balance impact)
    ACCOUNT    // Transfer from a specific bank/cash account
}

data class TradeIntent(
    val type: TradeType = TradeType.AssetTrade.BUY,
    val date: LocalDateTime = LocalDateTime.now(),

    // The subaccount where the asset quantity is recorded
    val targetAccountId: Long? = null,
    // The Asset being acquired or disposed of (e.g., AAPL, BTC)
    val targetAsset: CurrencyUnit,
    val quantity: BigDecimal = BigDecimal.ZERO,

    val price: BigDecimal = BigDecimal.ZERO,

    val fundingSource: FundingSource = FundingSource.PORTFOLIO,
    val fundingAccountId: Long? = null,
    val fee: BigDecimal = BigDecimal.ZERO,

    val comment: String = "",
    val payeeId: Long? = null,
)
