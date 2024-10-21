package org.totschnig.myexpenses.ui

import android.content.Context
import android.content.ContextWrapper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.databinding.ExchangeRateBinding
import org.totschnig.myexpenses.databinding.ExchangeRatesBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler.Companion.report
import org.totschnig.myexpenses.util.ui.setHintForA11yOnly
import org.totschnig.myexpenses.viewmodel.ExchangeRateViewModel
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

class ExchangeRateEdit(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    fun interface ExchangeRateWatcher {
        fun afterExchangeRateChanged(rate: BigDecimal, inverse: BigDecimal)
    }

    val rate1Edit: AmountEditText
    val rate2Edit: AmountEditText
    private var exchangeRateWatcher: ExchangeRateWatcher? = null
    private var blockWatcher = false
    private lateinit var viewModel: ExchangeRateViewModel
    private lateinit var firstCurrency: CurrencyUnit
    private lateinit var secondCurrency: CurrencyUnit
    private val binding = ExchangeRatesBinding.inflate(LayoutInflater.from(getContext()), this)
    fun setExchangeRateWatcher(exchangeRateWatcher: ExchangeRateWatcher?) {
        this.exchangeRateWatcher = exchangeRateWatcher
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupViewModel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel.clear()
    }

    private fun findLifecycleOwner(context: Context?): LifecycleOwner? {
        if (context is LifecycleOwner) {
            return context
        }
        return if (context is ContextWrapper) {
            findLifecycleOwner(context.baseContext)
        } else null
    }

    private fun setupViewModel() {
        val context = context
        viewModel = ExchangeRateViewModel((context.applicationContext as MyApplication))
        val lifecycleOwner = findLifecycleOwner(context)
        if (lifecycleOwner != null) {
            viewModel.getData().observe(lifecycleOwner) { result: Double? ->
                rate2Edit.setAmount(
                    BigDecimal.valueOf(
                        result!!
                    )
                )
            }
            viewModel.getError().observe(lifecycleOwner) { message: String -> complain(message) }
        } else {
            report(Exception("No LifecycleOwner found"))
        }
    }

    fun setBlockWatcher(blockWatcher: Boolean) {
        this.blockWatcher = blockWatcher
    }

    init {
        binding.ivDownload.getRoot().setOnClickListener {
            if (::firstCurrency.isInitialized && ::secondCurrency.isInitialized && ::viewModel.isInitialized) {
                viewModel.loadExchangeRate(firstCurrency.code, secondCurrency.code, host.date)
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
            exchangeRate = a2Abs.divide(a1Abs, MathContext.DECIMAL32)
            inverseExchangeRate = a1Abs.divide(a2Abs, MathContext.DECIMAL32)
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
            rate2Edit.setAmount(calculateInverse(rate))
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
            firstCurrency  = it
        }
        second?.let {
            secondCurrency = it
        }
        if (::firstCurrency.isInitialized && ::secondCurrency.isInitialized) {
            setSymbols(binding.ExchangeRate1, firstCurrency.symbol, secondCurrency.symbol)
            setSymbols(binding.ExchangeRate2, secondCurrency.symbol, firstCurrency.symbol)
            rate1Edit.setHintForA11yOnly(context.getString(
                R.string.content_description_exchange_rate,
                firstCurrency.description,
                secondCurrency.description
            ))
            rate2Edit.setHintForA11yOnly(context.getString(
                R.string.content_description_exchange_rate,
                secondCurrency.description,
                firstCurrency.description
            ))
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
            if (exchangeRateWatcher != null) {
                if (isMain) {
                    exchangeRateWatcher!!.afterExchangeRateChanged(inputRate, inverseInputRate)
                } else {
                    exchangeRateWatcher!!.afterExchangeRateChanged(inverseInputRate, inputRate)
                }
            }
            blockWatcher = false
        }
    }

    fun getRate(inverse: Boolean): BigDecimal? {
        return (if (inverse) rate2Edit else rate1Edit).getAmount(false).getOrNull()
    }

    private fun calculateInverse(input: BigDecimal?): BigDecimal {
        return if (input!!.compareTo(nullValue) != 0) BigDecimal(1).divide(
            input,
            MathContext.DECIMAL32
        ) else nullValue
    }

    private fun complain(message: String) {
        val host = host
        (host as BaseActivity).showSnackBar(message, Snackbar.LENGTH_LONG)
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
        val date: LocalDate
    }

    companion object {
        private const val EXCHANGE_RATE_FRACTION_DIGITS = 20
        private val nullValue = BigDecimal(0)
    }
}
