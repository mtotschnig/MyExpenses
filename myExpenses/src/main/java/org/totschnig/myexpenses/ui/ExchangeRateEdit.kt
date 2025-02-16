package org.totschnig.myexpenses.ui

import android.content.Context
import android.content.ContextWrapper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.databinding.ExchangeRateBinding
import org.totschnig.myexpenses.databinding.ExchangeRatesBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.ui.setEnabledWithColor
import org.totschnig.myexpenses.util.ui.setHintForA11yOnly
import timber.log.Timber
import java.math.BigDecimal
import java.math.MathContext

class ExchangeRateEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    fun interface ExchangeRateWatcher {
        fun afterExchangeRateChanged(rate: BigDecimal, inverse: BigDecimal)
    }

    val rate1Edit: AmountEditText
    val rate2Edit: AmountEditText
    val downloadButton: ImageView
    private lateinit var exchangeRateWatcher: ExchangeRateWatcher
    private var blockWatcher = false
    private lateinit var otherCurrency: CurrencyUnit
    private lateinit var baseCurrency: CurrencyUnit
    private val binding = ExchangeRatesBinding.inflate(LayoutInflater.from(getContext()), this)
    fun setExchangeRateWatcher(exchangeRateWatcher: ExchangeRateWatcher) {
        this.exchangeRateWatcher = exchangeRateWatcher
    }

    val lifecycleScope: CoroutineScope?
        get() = findViewTreeLifecycleOwner()?.lifecycleScope



    fun setBlockWatcher(blockWatcher: Boolean) {
        this.blockWatcher = blockWatcher
    }

    init {
        downloadButton = binding.ivDownload.getRoot()
        downloadButton.setOnClickListener {
            if (::otherCurrency.isInitialized && ::baseCurrency.isInitialized) {
                val providers =  ExchangeRateSource.configuredSources(context.injector.prefHandler()).toList()
                if (providers.size == 1) {
                    lifecycleScope?.launch {
                        handleResult(host.loadExchangeRate(otherCurrency.code, baseCurrency.code,  providers.first()))
                    }
                } else {
                    PopupMenu(context, downloadButton).apply {
                        setOnMenuItemClickListener { item ->
                            lifecycleScope?.launch {
                                handleResult(host.loadExchangeRate(otherCurrency.code, baseCurrency.code,
                                    providers[item.itemId]
                                ))
                            }
                            true
                        }
                        providers.forEachIndexed { index, s ->
                            menu.add(Menu.NONE, index, Menu.NONE, s.id)
                        }
                        show()
                    }
                }
            }
        }
        rate1Edit = binding.ExchangeRate1.ExchangeRateText
        rate1Edit.id = R.id.ExchangeRateEdit1
        rate2Edit = binding.ExchangeRate2.ExchangeRateText
        rate2Edit.id = R.id.ExchangeRateEdit2
        rate1Edit.fractionDigits = EXCHANGE_RATE_FRACTION_DIGITS
        rate2Edit.fractionDigits = EXCHANGE_RATE_FRACTION_DIGITS
        rate1Edit.addTextChangedListener(LinkedExchangeRateTextWatcher(true))
        rate2Edit.addTextChangedListener(LinkedExchangeRateTextWatcher(false))
    }

    /**
     * does not trigger call to registered ExchangeRateWatcher calculates rates based on two values
     */
    fun calculateAndSetRate(amount1: BigDecimal?, amount2: BigDecimal?) {
        blockWatcher = true
        val exchangeRate: BigDecimal
        val inverseExchangeRate: BigDecimal
        if (amount1 != null && amount2 != null && amount1.compareTo(nullValue) != 0 && amount2.compareTo(
                nullValue
            ) != 0
        ) {
            val a2Abs = amount2.abs()
            val a1Abs = amount1.abs()
            exchangeRate = a2Abs.divide(a1Abs, MathContext.DECIMAL64)
            inverseExchangeRate = a1Abs.divide(a2Abs, MathContext.DECIMAL64)
            rate1Edit.setAmount(exchangeRate)
            rate2Edit.setAmount(inverseExchangeRate)
        }
        blockWatcher = false
    }

    /**
     * does not trigger call to registered ExchangeRateWatcher; calculates inverse rate, and sets both values
     */
    fun setRate(rate: BigDecimal?, blockWatcher: Boolean) {
        if (rate != null) {
            if (blockWatcher) {
                this.blockWatcher = true
            }
            rate1Edit.setAmount(rate)
            if (blockWatcher) {//watcher takes care of setting inverse rate
                rate2Edit.setAmount(calculateInverse(rate))
            }
            this.blockWatcher = false
        }
    }

    override fun setEnabled(enabled: Boolean) {
        rate1Edit.setEnabled(enabled)
        rate2Edit.setEnabled(enabled)
        super.setEnabled(enabled)
    }

    fun setCurrencies(first: CurrencyUnit?, second: CurrencyUnit?) {
        first?.let {
            otherCurrency  = it
        }
        second?.let {
            baseCurrency = it
        }
        if (::otherCurrency.isInitialized && ::baseCurrency.isInitialized) {
            setSymbols(binding.ExchangeRate1, otherCurrency.symbol, baseCurrency.symbol)
            setSymbols(binding.ExchangeRate2, baseCurrency.symbol, otherCurrency.symbol)
            rate1Edit.setHintForA11yOnly(context.getString(
                R.string.content_description_exchange_rate,
                otherCurrency.description,
                baseCurrency.description
            ))
            rate2Edit.setHintForA11yOnly(context.getString(
                R.string.content_description_exchange_rate,
                baseCurrency.description,
                otherCurrency.description
            ))
            downloadButton.setEnabledWithColor(otherCurrency.code != baseCurrency.code)
        }
    }

    private fun setSymbols(group: ExchangeRateBinding, symbol1: String, symbol2: String) {
        group.ExchangeRateLabel1.text = String.format("1 %s =", symbol1)
        group.ExchangeRateLabel2.text = symbol2
    }

    private inner class LinkedExchangeRateTextWatcher(
        /**
         * true if we are linked to exchange rate where unit is from account currency
         */
        private val isMain: Boolean,
    ) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            if (blockWatcher) return
            blockWatcher = true
            var inputRate = getRate(!isMain)
            if (inputRate == null) inputRate = nullValue
            val inverseInputRate = calculateInverse(inputRate)
            (if (isMain) rate2Edit else rate1Edit).setAmount(inverseInputRate)
            if (::exchangeRateWatcher.isInitialized) {
                if (isMain) {
                    exchangeRateWatcher.afterExchangeRateChanged(inputRate, inverseInputRate)
                } else {
                    exchangeRateWatcher.afterExchangeRateChanged(inverseInputRate, inputRate)
                }
            }
            blockWatcher = false
        }
    }

    fun getRate(inverse: Boolean): BigDecimal? {
        return (if (inverse) rate2Edit else rate1Edit).getAmount(false).getOrNull()
    }

    private fun calculateInverse(input: BigDecimal): BigDecimal {
        return if (input.compareTo(nullValue) != 0) BigDecimal(1).divide(
            input,
            MathContext.DECIMAL64
        ) else nullValue
    }

    private fun handleResult(result: Result<Double>) {
        result.onSuccess {
            Timber.d("result: $it")
            rate1Edit.setAmount(BigDecimal.valueOf(it))
        }.onFailure {
            complain(it.safeMessage)
        }
    }

    private fun complain(message: String) {
        (this.host as BaseActivity).showSnackBar(message, Snackbar.LENGTH_LONG)
    }

    private val host: Host
        get() {
            var context = context
            while (context is ContextWrapper) {
                if (context is Host) {
                    return context
                }
                context = context.baseContext
            }
            throw IllegalStateException("Host context does not implement interface")
        }

    interface Host {
        suspend fun loadExchangeRate(other: String, base: String, source: ExchangeRateSource): Result<Double>
    }

    companion object {
        private const val EXCHANGE_RATE_FRACTION_DIGITS = 20
        private val nullValue = BigDecimal(0)
    }
}
