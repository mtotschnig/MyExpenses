package org.totschnig.myexpenses.util

import android.content.ContentResolver
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency

interface ICurrencyFormatter {
    fun formatCurrency(
        amount: BigDecimal,
        currency: CurrencyUnit,
        configure: ((DecimalFormat) -> Unit)? = null
    ): String

    fun invalidate(contentResolver: ContentResolver, currency: String? = null) {}
}

object DebugCurrencyFormatter : ICurrencyFormatter {

    override fun formatCurrency(
        amount: BigDecimal,
        currency: CurrencyUnit,
        configure: ((DecimalFormat) -> Unit)?
    ) = "${currency.code} $amount"
}

/**
 * formats an amount with a currency
 * @return formatted string
 */
@JvmOverloads
fun ICurrencyFormatter.formatMoney(
    money: Money,
    configure: ((DecimalFormat) -> Unit)? = null
): String {
    val amount = money.amountMajor
    return formatCurrency(amount, money.currencyUnit, configure)
}

/**
 * utility method that calls formatters for amount this method can be called
 * directly with Long values retrieved from db
 *
 * @return formatted string
 */
fun ICurrencyFormatter.convAmount(amount: Long, currency: CurrencyUnit): String {
    return formatMoney(Money(currency, amount))
}

open class CurrencyFormatter(
    private val prefHandler: PrefHandler,
    private val application: MyApplication
) : ICurrencyFormatter {
    private val numberFormats: MutableMap<String, NumberFormat> = HashMap()

    override fun invalidate(contentResolver: ContentResolver, currency: String?) {
        if (currency == null) {
            numberFormats.clear()
        } else {
            numberFormats.remove(currency)
        }
        notifyUris(contentResolver)
    }

    private fun notifyUris(contentResolver: ContentResolver) {
        contentResolver.notifyChange(TransactionProvider.TEMPLATES_URI, null, false)
        contentResolver.notifyChange(TransactionProvider.TRANSACTIONS_URI, null, false)
        contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
        contentResolver.notifyChange(TransactionProvider.UNCOMMITTED_URI, null, false)
    }

    private fun initNumberFormat(): NumberFormat {
        val prefFormat = prefHandler.getString(PrefKey.CUSTOM_DECIMAL_FORMAT, "")
        if ("" != prefFormat) {
            val nf = DecimalFormat()
            try {
                nf.applyLocalizedPattern(prefFormat)
                return nf
            } catch (_: IllegalArgumentException) {
                //fallback to default currency instance
            }
        }
        return NumberFormat.getCurrencyInstance(application.userPreferredLocale)
    }

    private fun getNumberFormat(currencyUnit: CurrencyUnit): NumberFormat {
        var numberFormat = numberFormats[currencyUnit.code]
        if (numberFormat == null) {
            numberFormat = initNumberFormat()
            val fractionDigits = currencyUnit.fractionDigits
            try {
                numberFormat.currency = Currency.getInstance(currencyUnit.code)
            } catch (_: Exception) { /*Custom locale}*/
            }
            val currencySymbol = currencyUnit.symbol
            val decimalFormatSymbols = (numberFormat as DecimalFormat).decimalFormatSymbols
            decimalFormatSymbols.currencySymbol = currencySymbol
            decimalFormatSymbols.minusSign = '\u2212'
            numberFormat.decimalFormatSymbols = decimalFormatSymbols
            if (fractionDigits <= 3) {
                numberFormat.minimumFractionDigits = fractionDigits
            }
            numberFormat.maximumFractionDigits = fractionDigits
            numberFormats[currencyUnit.code] = numberFormat
        }
        return numberFormat
    }

    override fun formatCurrency(
        amount: BigDecimal,
        currency: CurrencyUnit,
        configure: ((DecimalFormat) -> Unit)?
    ): String {
        return getNumberFormat(currency).let { nf ->
            if (configure != null && nf is DecimalFormat) {
                (nf.clone() as DecimalFormat).also { configure(it) }
            } else nf
        }.format(amount)
    }
}