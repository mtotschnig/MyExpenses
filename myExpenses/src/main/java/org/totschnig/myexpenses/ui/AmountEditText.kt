package org.totschnig.myexpenses.ui

import android.content.Context
import android.os.Parcelable
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Objects

class AmountEditText(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private val decimalSeparator: Char = Utils.getDefaultDecimalSeparator()

    @State
    var fractionDigits = -1
        set(value) {
            if (field != value) {
                val symbols = DecimalFormatSymbols()
                symbols.decimalSeparator = decimalSeparator
                var pattern = "#0"
                if (value > 0) {
                    pattern += "." + String(CharArray(value)).replace("\u0000", "#")
                }
                numberFormat = DecimalFormat(pattern, symbols)
                numberFormat.isGroupingUsed = false
                configDecimalSeparator(decimalSeparator, value)
                //if the new configuration has less fraction digits, we might have to truncate the input
                if (field != -1 && field > value) {
                    val currentText = Objects.requireNonNull(text).toString()
                    val decimalSeparatorIndex = currentText.indexOf(decimalSeparator)
                    if (decimalSeparatorIndex != -1) {
                        val minorPart = currentText.substring(decimalSeparatorIndex + 1)
                        if (minorPart.length > value) {
                            var newText = currentText.substring(0, decimalSeparatorIndex)
                            if (value > 0) {
                                newText += decimalSeparator.toString() + minorPart.substring(
                                    0,
                                    value
                                )
                            }
                            setText(newText)
                        }
                    }
                }
                field = value
            }
        }
    private var numberFormat = DecimalFormat()

    override fun onSaveInstanceState(): Parcelable {
        return StateSaver.saveInstanceState(this, super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(StateSaver.restoreInstanceState(this, state))
    }

    fun setAmount(amount: BigDecimal) {
        setText(numberFormat.format(amount))
    }

    fun getAmount(showToUser: Boolean) = runCatching {
        val strAmount = Objects.requireNonNull(text).toString()
        if (strAmount == "" || (strAmount.length == 1 && strAmount.first() == decimalSeparator))  {
            if (showToUser) error = context.getString(R.string.required)
            null
        } else Utils.validateNumber(numberFormat, strAmount).also { amount ->
            if (amount == null) {
                val errorMessage =
                    "Expected format ${numberFormat.toPattern()}, instead got $strAmount"
                if (showToUser) {
                    error = errorMessage
                    throw Exception(errorMessage).also {
                        CrashHandler.report(it)
                    }
                }
            }
        }
    }

    fun getAmount(currencyUnit: CurrencyUnit) =
        getAmount(true).mapCatching { amount ->
            amount?.let {
                try {
                    Money(currencyUnit, it)
                } catch (e: ArithmeticException) {
                    error = "Number too large."
                    throw e
                }
            }
        }


    private fun configDecimalSeparator(decimalSeparator: Char, fractionDigits: Int) {
        // TODO we should take into account the arab separator as well
        val otherSeparator = if (decimalSeparator == '.') ',' else '.'
        setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        filters = arrayOf(
            FractionDigitsInputFilter(decimalSeparator, otherSeparator, fractionDigits),
            LengthFilter(16)
        )
    }
}
