package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import java.math.BigDecimal
import java.time.LocalDateTime

sealed class TradeType(@param:StringRes val label: Int) : Parcelable {
    sealed class AssetTrade(label: Int) : TradeType(label) {
        @Parcelize
        data object BUY : AssetTrade(R.string.trade_buy)
        @Parcelize
        data object SELL : AssetTrade(R.string.trade_sell)
    }

    sealed class CashMovement(label: Int) : TradeType(label) {
        @Parcelize
        data object DEPOSIT : CashMovement(R.string.trade_deposit)
        @Parcelize
        data object WITHDRAW : CashMovement(R.string.trade_withdraw)
    }

    companion object {
        val entries by lazy {
            listOf(AssetTrade.BUY, AssetTrade.SELL, CashMovement.DEPOSIT, CashMovement.WITHDRAW)
        }
    }
}

enum class FundingSource {
    PORTFOLIO, // Uses the portfolio's cash balance
    EXTERNAL,  // Not tracked in the app (no balance impact)
    ACCOUNT    // Transfer from a specific bank/cash account
}

data class TradeIntent(
    // The Asset being acquired or disposed of (e.g., AAPL, BTC)
    val targetAsset: CurrencyUnit,
    val type: TradeType,
    val date: LocalDateTime,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val fundingSource: FundingSource = FundingSource.PORTFOLIO,
    val fundingAccountId: Long?,
    val fee: BigDecimal,
    val comment: String = "",
    val linkedTransactionId: Long? = null,
) {
    init {
        if (type is TradeType.CashMovement) {
            require(fundingSource != FundingSource.PORTFOLIO)
        }
    }
}
