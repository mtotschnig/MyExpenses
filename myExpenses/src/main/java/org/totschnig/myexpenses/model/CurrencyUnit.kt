package org.totschnig.myexpenses.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Currency

enum class CommodityType {
    FIAT,
    SECURITY,
    CRYPTO
}

@Immutable
@Parcelize
data class CurrencyUnit(
    val code: String,
    val symbol: String,
    val fractionDigits: Int,
    val description: String,
    val commodityType: CommodityType = CommodityType.FIAT,
    val databaseId: Long = -1
) : Parcelable, AccountGroupingKey {
    @VisibleForTesting
    constructor(currency: Currency) : this(
        currency.currencyCode, currency.symbol, currency.defaultFractionDigits,
        currency.displayName
    )

    constructor(code: String, symbol: String, fractionDigits: Int) : this(
        code,
        symbol,
        fractionDigits,
        code
    )

    @IgnoredOnParcel
    override val id: String = code
    override fun title(context: Context) = org.totschnig.myexpenses.viewmodel.data.Currency.create(code, context).toString()

    companion object {
        val DebugInstance by lazy { CurrencyUnit(Currency.getInstance("EUR")) }
    }
}