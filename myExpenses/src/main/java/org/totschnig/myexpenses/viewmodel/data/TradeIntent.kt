package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import java.math.BigDecimal
import java.time.LocalDateTime

sealed interface TradeType : Parcelable {
    @get:StringRes
    val label: Int
    val isIncoming: Boolean

    sealed class AssetTrade(override val label: Int, override val isIncoming: Boolean) : TradeType {
        @Parcelize data object BUY : AssetTrade(R.string.trade_buy, true)
        @Parcelize data object SELL : AssetTrade(R.string.trade_sell, false)
    }

    sealed class CashMovement(override val label: Int, override val isIncoming: Boolean) : TradeType {
        @Parcelize data object DEPOSIT : CashMovement(R.string.trade_deposit, true)
        @Parcelize data object WITHDRAW : CashMovement(R.string.trade_withdraw, false)
    }

@Parcelize
    data class Transfer(override val isIncoming: Boolean) : TradeType {
        @IgnoredOnParcel
        override val label: Int = R.string.trade_transfer
    }

    companion object {
        val entries: List<TradeType> by lazy {
            listOf(AssetTrade.BUY, AssetTrade.SELL, CashMovement.DEPOSIT, CashMovement.WITHDRAW, Transfer(false))
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
    val quantity: Money,
    val price: BigDecimal,
    val principal: Money,
    val fundingSource: FundingSource = FundingSource.PORTFOLIO,
    val peerAccountId: Long?,
    val fee: Money,
    val comment: String = "",
    val linkedTransactionId: Long? = null,
    //edit of existing trade
    val tradeId: Long? = null,
) {
    init {
        if (type is TradeType.CashMovement) {
            require(fundingSource != FundingSource.PORTFOLIO)
        }
    }
}
