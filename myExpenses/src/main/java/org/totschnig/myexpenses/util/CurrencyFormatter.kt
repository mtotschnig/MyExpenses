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
        configure: ((DecimalFormat) -> Unit)? = null,
    ): String

    fun invalidate(contentResolver: ContentResolver, currency: String? = null) {}
}

object DebugCurrencyFormatter : ICurrencyFormatter {

    override fun formatCurrency(
        amount: BigDecimal,
        currency: CurrencyUnit,
        configure: ((DecimalFormat) -> Unit)?,
    ) = "${currency.code} $amount"
}

/**
 * formats an amount with a currency
 * @return formatted string
 */
@JvmOverloads
fun ICurrencyFormatter.formatMoney(
    money: Money,
    configure: ((DecimalFormat) -> Unit)? = null,
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
    private val application: MyApplication,
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
    }

    /**
     * @return pair of NumberFormat and true if format is default or false if it is custom
     */
    private fun initNumberFormat(): Pair<NumberFormat, Boolean> {
        val prefFormat = prefHandler.getString(PrefKey.CUSTOM_DECIMAL_FORMAT, "")
        if ("" != prefFormat) {
            val nf = DecimalFormat()
            try {
                nf.applyLocalizedPattern(prefFormat)
                return nf to false
            } catch (_: IllegalArgumentException) {
                //fallback to default currency instance
            }
        }
        return NumberFormat.getCurrencyInstance(application.userPreferredLocale) to true
    }

    private fun getNumberFormat(currencyUnit: CurrencyUnit): NumberFormat {
        return numberFormats.getOrPut(currencyUnit.code) {
            val (newFormat, isDefault) = initNumberFormat()
            val fractionDigits = currencyUnit.fractionDigits
            (newFormat as DecimalFormat).apply {
                try {
                    currency = Currency.getInstance(currencyUnit.code)
                } catch (_: Exception) {/*Custom locale}*/
                }
                decimalFormatSymbols = decimalFormatSymbols.apply {
                    currencySymbol = currencyUnit.symbol
                    minusSign = '\u2212'
                }
                // Only set minimumFractionDigits for the default format, as a custom
                // format may have its own rules.
                if (isDefault && fractionDigits <= 3) {
                    minimumFractionDigits = fractionDigits
                }
                // Always set the maximum, as this is determined by the currency's definition.
                maximumFractionDigits = fractionDigits
            }
        }
    }

    override fun formatCurrency(
        amount: BigDecimal,
        currency: CurrencyUnit,
        configure: ((DecimalFormat) -> Unit)?,
    ): String {
        return getNumberFormat(currency).let { nf ->
            if (configure != null && nf is DecimalFormat) {
                (nf.clone() as DecimalFormat).also { configure(it) }
            } else nf
        }.format(amount)
    }
}