package org.totschnig.myexpenses.model

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import java.util.Currency

enum class CommodityType(
    @param:StringRes val labelSingular: Int,
    @param:StringRes val labelPlural: Int,
) {
    FIAT(R.string.currency, R.string.asset_type_fiat_plural),
    SECURITY(R.string.asset_type_security, R.string.asset_type_security_plural),
    CRYPTO(R.string.asset_type_crypto, R.string.asset_type_crypto);

    companion object {
        fun title(resources: Resources) = entries.joinToString(" · ") { resources.getString(it.labelPlural) }
    }
}

@Immutable
@Parcelize
data class CurrencyUnit(
    val code: String,
    val symbol: String,
    val fractionDigits: Int,
    val description: String,
    val commodityType: CommodityType = CommodityType.FIAT,
    val databaseId: Long = -1,
) : Parcelable, AccountGroupingKey {
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
    override fun title(context: Context) =
        org.totschnig.myexpenses.viewmodel.data.Currency.create(code, context).toString()

    companion object {
        val DebugInstance by lazy { CurrencyUnit(Currency.getInstance("EUR")) }
    }
}