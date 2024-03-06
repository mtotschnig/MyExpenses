package org.totschnig.myexpenses.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBinding
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.AmountInputAlternateBinding
import org.totschnig.myexpenses.databinding.AmountInputBinding
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import java.math.BigDecimal

class AmountInput(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    fun typeButton(): CompoundButton {
        return (if (viewBinding is AmountInputAlternateBinding) (viewBinding as AmountInputAlternateBinding).TaType else (viewBinding as AmountInputBinding?)!!.TaType)
            .getRoot()
    }

    private fun amountEditText(): AmountEditText {
        return (if (viewBinding is AmountInputAlternateBinding) (viewBinding as AmountInputAlternateBinding).AmountEditText else (viewBinding as AmountInputBinding?)!!.AmountEditText)
            .getRoot()
    }

    private fun calculator(): ImageView {
        return (if (viewBinding is AmountInputAlternateBinding) (viewBinding as AmountInputAlternateBinding).Calculator else (viewBinding as AmountInputBinding?)!!.Calculator)
            .getRoot()
    }

    private fun exchangeRateEdit(): ExchangeRateEdit {
        return (if (viewBinding is AmountInputAlternateBinding) (viewBinding as AmountInputAlternateBinding).AmountExchangeRate else (viewBinding as AmountInputBinding?)!!.AmountExchangeRate)
            .getRoot()
    }

    private var viewBinding: ViewBinding? = null
    private var withTypeSwitch = false
    private var withCurrencySelection = false
    private var withExchangeRate = false
    private var typeChangedListener: TypeChangedListener? = null
    private var compoundResultOutListener: CompoundResultOutListener? = null
    private var compoundResultInput: BigDecimal? = null
    private var initialized = false
    private lateinit var currencyAdapter: CurrencyAdapter
    val currencySpinner: SpinnerHelper

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AmountInput)
        withCurrencySelection = ta.getBoolean(R.styleable.AmountInput_withCurrencySelection, false)
        withExchangeRate = ta.getBoolean(R.styleable.AmountInput_withExchangeRate, false)
        val alternateLayout = ta.getBoolean(R.styleable.AmountInput_alternateLayout, false)
        val inflater = LayoutInflater.from(context)
        viewBinding = if (alternateLayout) AmountInputAlternateBinding.inflate(
            inflater,
            this
        ) else AmountInputBinding.inflate(inflater, this)
        currencySpinner = SpinnerHelper(
            (if (viewBinding is AmountInputAlternateBinding) (viewBinding as AmountInputAlternateBinding).AmountCurrency else (viewBinding as AmountInputBinding).AmountCurrency)
                .getRoot()
        )
        updateChildContentDescriptions()
        setWithTypeSwitch(ta.getBoolean(R.styleable.AmountInput_withTypeSwitch, true))
        ta.recycle()
        if (withCurrencySelection) {
            currencyAdapter =
                object : CurrencyAdapter(getContext(), android.R.layout.simple_spinner_item) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        view.setPadding(
                            view.getPaddingLeft(),
                            view.paddingTop,
                            0,
                            view.paddingBottom
                        )
                        (view as TextView).text = getItem(position)!!.code
                        return view
                    }
                }
            currencySpinner.adapter = currencyAdapter
            currencySpinner.setOnItemSelectedListener(object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val currency = (currencySpinner.selectedItem as Currency?)!!.code
                    val currencyUnit: CurrencyUnit = host.currencyContext.get(currency)
                    configureCurrency(currencyUnit)
                    host.onCurrencySelectionChanged(currencyUnit)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
        } else {
            currencySpinner.spinner.visibility = GONE
        }
        if (withExchangeRate) {
            exchangeRateEdit().setExchangeRateWatcher { _: BigDecimal?, _: BigDecimal? ->
                onCompoundResultOutput()
                onCompoundResultInput()
            }
        } else {
            exchangeRateEdit().visibility = GONE
        }
        calculator().setOnClickListener { host.showCalculator(validate(false), id) }
        initialized = true
    }

    override fun setContentDescription(contentDescription: CharSequence) {
        super.setContentDescription(contentDescription)
        if (initialized) {
            updateChildContentDescriptions()
        }
    }

    var exchangeRate: BigDecimal?
        get() = exchangeRateEdit().getRate(false)
        set(rate) {
            exchangeRateEdit().setRate(rate, false)
        }

    private fun updateChildContentDescriptions() {
        //Edit Text does not use content description once it holds content. It is hence needed to point a textView
        //in the neighborhood of this AmountInput directly to amountEdiText with android:labelFor="@id/AmountEditText"
        //setContentDescriptionForChild(amountEditText, null);
        setContentDescriptionForChild(
            calculator(),
            context.getString(R.string.content_description_calculator)
        )
        setContentDescriptionForChild(
            currencySpinner.spinner,
            context.getString(R.string.currency)
        )
        setContentDescriptionForTypeSwitch()
    }

    private fun setContentDescriptionForChild(view: View, contentDescription: CharSequence?) {
        view.setContentDescription(
            if (contentDescription == null) getContentDescription() else String.format(
                "%s : %s",
                getContentDescription(),
                contentDescription
            )
        )
    }

    private fun setContentDescriptionForTypeSwitch() {
        setContentDescriptionForChild(
            typeButton(), context.getString(
                if (typeButton().isChecked) R.string.income else R.string.expense
            )
        )
    }

    fun setTypeChangedListener(typeChangedListener: TypeChangedListener) {
        this.typeChangedListener = typeChangedListener
    }

    fun setCompoundResultOutListener(compoundResultOutListener: CompoundResultOutListener) {
        compoundResultInput = null
        this.compoundResultOutListener = compoundResultOutListener
        amountEditText().addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                onCompoundResultOutput()
            }
        })
    }

    fun setCompoundResultInput(input: BigDecimal?) {
        compoundResultInput = input
        onCompoundResultInput()
    }

    private fun onCompoundResultOutput() {
        if (compoundResultOutListener == null) return
        val input = validate(false)
        val rate = exchangeRateEdit().getRate(false)
        if (input != null && rate != null) {
            compoundResultOutListener!!.onResultChanged(input.multiply(rate))
        }
    }

    private fun onCompoundResultInput() {
        if (compoundResultInput != null) {
            val rate = exchangeRateEdit().getRate(false)
            if (rate != null) {
                setAmount(compoundResultInput!!.multiply(rate), false)
            }
        }
    }

    fun addTextChangedListener(textWatcher: TextWatcher?) {
        amountEditText().addTextChangedListener(textWatcher)
    }

    fun setFractionDigits(i: Int) {
        amountEditText().fractionDigits = i
    }

    fun setWithTypeSwitch(withTypeSwitch: Boolean) {
        this.withTypeSwitch = withTypeSwitch
        val button = typeButton()
        if (withTypeSwitch) {
            button.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                setContentDescriptionForTypeSwitch()
                if (typeChangedListener != null) {
                    typeChangedListener!!.onTypeChanged(isChecked)
                }
            }
            button.visibility = VISIBLE
        } else {
            button.setOnCheckedChangeListener(null)
            button.visibility = GONE
        }
    }

    fun setAmount(amount: BigDecimal) {
        setAmount(amount, true)
    }

    fun setAmount(amount: BigDecimal, updateType: Boolean) {
        amountEditText().error = null
        amountEditText().setAmount(amount.abs())
        if (updateType) {
            type = amount.signum() > -1
        }
    }

    fun setRaw(text: String?) {
        amountEditText().setText(text)
    }

    fun clear() {
        amountEditText().setText("")
    }

    fun validate(showToUser: Boolean): BigDecimal? {
        return amountEditText().validateLegacy(showToUser)
    }

    val typedValue: BigDecimal
        get() = getTypedValue(ifPresent = false, showToUser = false)!!

    fun getTypedValue(ifPresent: Boolean, showToUser: Boolean): BigDecimal? {
        val bigDecimal = validate(showToUser)
        if (bigDecimal == null) {
            return if (ifPresent) null else BigDecimal.ZERO
        } else if (!type) {
            return bigDecimal.negate()
        }
        return bigDecimal
    }

    var type: Boolean
        get() = !withTypeSwitch || typeButton().isChecked
        set(type) {
            typeButton().setChecked(type)
        }

    fun hideTypeButton() {
        typeButton().visibility = GONE
    }

    fun setTypeEnabled(enabled: Boolean) {
        typeButton().setEnabled(enabled)
    }

    fun toggle() {
        typeButton().toggle()
    }

    fun setCurrencies(currencies: List<Currency?>) {
        currencyAdapter.addAll(currencies)
        currencySpinner.setSelection(0)
    }

    private fun configureCurrency(currencyUnit: CurrencyUnit) {
        setFractionDigits(currencyUnit.fractionDigits)
        configureExchange(currencyUnit, null)
    }

    fun setSelectedCurrency(currency: CurrencyUnit) {
        currencySpinner.setSelection(
            currencyAdapter.getPosition(
                create(
                    currency.code,
                    context
                )
            )
        )
        configureCurrency(currency)
    }

    fun configureExchange(currencyUnit: CurrencyUnit?, homeCurrency: CurrencyUnit?) {
        if (withExchangeRate) {
            exchangeRateEdit().setCurrencies(currencyUnit, homeCurrency)
        }
    }

    /**
     * sets the second currency on the exchangeEdit, the first one taken from the currency selector
     */
    fun configureExchange(currencyUnit: CurrencyUnit?) {
        if (withCurrencySelection) {
            val selectedCurrency = selectedCurrency
            configureExchange(
                if (selectedCurrency != null) host.currencyContext[selectedCurrency.code] else null,
                currencyUnit
            )
        }
    }

    val selectedCurrency: Currency?
        get() = currencySpinner.selectedItem as Currency?

    fun disableCurrencySelection() {
        currencySpinner.isEnabled = false
    }

    fun disableExchangeRateEdit() {
        exchangeRateEdit().setEnabled(false)
    }

    fun selectAll() {
        amountEditText().selectAll()
    }

    fun setError(error: CharSequence?) {
        amountEditText().error = error
    }

    /**
     * this amount input is supposed to output the application of the exchange rate to its amount
     * used for the original amount in [org.totschnig.myexpenses.activity.ExpenseEdit]
     */
    fun interface CompoundResultOutListener {
        fun onResultChanged(result: BigDecimal)
    }

    fun interface TypeChangedListener {
        fun onTypeChanged(type: Boolean)
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
        fun showCalculator(amount: BigDecimal?, id: Int)
        fun setFocusAfterRestoreInstanceState(focusView: Pair<Int, Int>?)
        fun onCurrencySelectionChanged(currencyUnit: CurrencyUnit)
        val currencyContext: CurrencyContext
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()!!
        val focusedChild = deepestFocusedChild
        return SavedState(
            superState, typeButton().onSaveInstanceState()!!,
            amountEditText().onSaveInstanceState(), currencySpinner.spinner.onSaveInstanceState()!!,
            exchangeRateEdit().getRate(false), focusedChild?.id ?: 0
        )
    }

    private val deepestFocusedChild: View?
        get() {
            var v: View? = this
            while (v != null) {
                if (v.isFocused) {
                    return v
                }
                v = if (v is ViewGroup) v.focusedChild else null
            }
            return null
        }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        typeButton().onRestoreInstanceState(savedState.typeButtonState)
        amountEditText().onRestoreInstanceState(savedState.amountEditTextState)
        currencySpinner.spinner.onRestoreInstanceState(savedState.currencySpinnerState)
        exchangeRateEdit().setRate(savedState.exchangeRateState, true)
        if (savedState.focusedId != 0) {
            @Suppress("unused")
            host.setFocusAfterRestoreInstanceState(Pair(id, savedState.focusedId))
        }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchThawSelfOnly(container)
    }

    internal class SavedState : BaseSavedState {
        val typeButtonState: Parcelable
        val amountEditTextState: Parcelable
        val currencySpinnerState: Parcelable
        val exchangeRateState: BigDecimal?
        val focusedId: Int

        constructor(source: Parcel) : super(source) {
            val classLoader = javaClass.getClassLoader()
            typeButtonState = source.readParcelable(classLoader)!!
            amountEditTextState = source.readParcelable(classLoader)!!
            currencySpinnerState = source.readParcelable(classLoader)!!
            exchangeRateState = source.readSerializable() as BigDecimal?
            focusedId = source.readInt()
        }

        constructor(
            superState: Parcelable,
            typeButtonState: Parcelable,
            amountEditTextState: Parcelable,
            currencySpinnerState: Parcelable,
            exchangeRateState: BigDecimal?,
            focusedId: Int
        ) : super(superState) {
            this.typeButtonState = typeButtonState
            this.amountEditTextState = amountEditTextState
            this.currencySpinnerState = currencySpinnerState
            this.exchangeRateState = exchangeRateState
            this.focusedId = focusedId
        }

        override fun writeToParcel(destination: Parcel, flags: Int) {
            super.writeToParcel(destination, flags)
            destination.writeParcelable(typeButtonState, flags)
            destination.writeParcelable(amountEditTextState, flags)
            destination.writeParcelable(currencySpinnerState, flags)
            destination.writeSerializable(exchangeRateState)
            destination.writeInt(focusedId)
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)

                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }
    }
}
