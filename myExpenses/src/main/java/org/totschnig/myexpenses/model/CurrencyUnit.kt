package org.totschnig.myexpenses.model

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.util.Currency

@Immutable
data class CurrencyUnit(
    val code: String,
    val symbol: String,
    val fractionDigits: Int,
    val description: String
) : Serializable, AccountGroupingKey {
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

    override val id: String = code
    override fun title(context: Context) = org.totschnig.myexpenses.viewmodel.data.Currency.create(code, context).toString()

    companion object {
        val DebugInstance by lazy { CurrencyUnit(Currency.getInstance("EUR")) }
    }
}