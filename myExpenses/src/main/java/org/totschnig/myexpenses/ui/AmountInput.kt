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
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBinding
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.AmountInputAlternateBinding
import org.totschnig.myexpenses.databinding.AmountInputBinding
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.ui.getActivity
import org.totschnig.myexpenses.util.ui.setHintForA11yOnly
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import timber.log.Timber
import java.math.BigDecimal

class AmountInput(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    fun typeButton() = ((viewBinding as? AmountInputAlternateBinding)?.TaType
        ?: (viewBinding as AmountInputBinding).TaType).root

    private fun amountEditText() = ((viewBinding as? AmountInputAlternateBinding)?.AmountEditText
        ?: (viewBinding as AmountInputBinding).AmountEditText).root

    private fun calculator() = ((viewBinding as? AmountInputAlternateBinding)?.Calculator
        ?: (viewBinding as AmountInputBinding).Calculator).root

    private fun exchangeRateEdit() = ((viewBinding as? AmountInputAlternateBinding)?.AmountExchangeRate
        ?: (viewBinding as AmountInputBinding).AmountExchangeRate).root

    private var viewBinding: ViewBinding? = null
    private var withTypeSwitch = false
    private var withCurrencySelection = false
    private var withExchangeRate = false
    private var typeChangedListener: TypeChangedListener? = null
    private var initialized = false
    private lateinit var currencyAdapter: CurrencyAdapter
    val currencySpinner: SpinnerHelper

    private var downStreamDependency: AmountInput? = null
    private var downStreamDependencyRef = -1
    private var upStreamDependency: AmountInput? = null
    private var upStreamDependencyRef = -1
    private var blockWatcher = false

    private val currencyContext
        get() = context.injector.currencyContext()

    /**
     * the user of this component will usually set an extensive content description explaining how to
     * use this component. In addition we need a short label (purpose) that will be used in the
     * content description of child elements
     */
    val purpose: CharSequence?

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
        purpose = ta.getText(R.styleable.AmountInput_purpose)
        updateChildContentDescriptions()
        setWithTypeSwitch(ta.getBoolean(R.styleable.AmountInput_withTypeSwitch, true))
        if (withCurrencySelection) {
            currencyAdapter =
                object : CurrencyAdapter(getContext(), android.R.layout.simple_spinner_item) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup,
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
                    id: Long,
                ) {
                    val currency = (currencySpinner.selectedItem as Currency).code
                    val currencyUnit: CurrencyUnit = currencyContext[currency]
                    configureCurrency(currencyUnit)
                    host.onCurrencySelectionChanged(currencyUnit)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
        } else {
            currencySpinner.spinner.visibility = GONE
        }
        if (withExchangeRate) {
            upStreamDependencyRef = ta.getResourceId(R.styleable.AmountInput_upStreamDependency, -1)
            downStreamDependencyRef =
                ta.getResourceId(R.styleable.AmountInput_downStreamDependency, -1)
            exchangeRateEdit().setExchangeRateWatcher { _, _ ->
                if (blockWatcher) return@setExchangeRateWatcher
                updateDownStream()
                updateFromUpStream()
            }
            amountEditText().addTextChangedListener(object : MyTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    if (blockWatcher) return
                    updateDownStream()
                    upStreamDependency?.let {
                        exchangeRateEdit().calculateAndSetRate(
                            it.getAmount(false),
                            getAmount(false)
                        )
                    }
                }
            })
        } else {
            exchangeRateEdit().visibility = GONE
        }
        calculator().setOnClickListener {
            host.showCalculator(getUntypedValue(false).getOrNull(), id)
        }
        ta.recycle()
        initialized = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        upStreamDependency = context.getActivity()?.findViewById(upStreamDependencyRef)
        downStreamDependency = context.getActivity()?.findViewById(downStreamDependencyRef)
        downStreamDependency?.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                if (!blockWatcher) updateFromDownStream()
            }
        })
        upStreamDependency?.addTextChangedListener(object : MyTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                updateFromUpStream()
            }
        })
        updateFromDownStream()
    }

    override fun setContentDescription(contentDescription: CharSequence) {
        super.setContentDescription(contentDescription)
        if (initialized) {
            updateChildContentDescriptions()
        }
    }

    val userSetExchangeRate: BigDecimal?
        get() = exchangeRateEdit().userSetExchangeRate

    var exchangeRate: BigDecimal?
        get() = exchangeRateEdit().getRate(false)
        set(rate) {
            exchangeRateEdit().setRate(rate, false)
        }

    private fun updateChildContentDescriptions() {
        setContentDescriptionForChild(amountEditText(), context.getString(R.string.amount))
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
        val parentContentDescription = purpose ?: this.contentDescription
        val childContentDescription =
            if (contentDescription == null) parentContentDescription else String.format(
                "%s : %s",
                parentContentDescription,
                contentDescription
            )
        if (view is EditText) {
            view.setHintForA11yOnly(childContentDescription)
        } else {
            view.contentDescription = childContentDescription
        }
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

    private fun updateDownStream() {
        downStreamDependency?.let {
            val input = getUntypedValue(false).getOrNull()
            val rate = exchangeRateEdit().getRate(false)
            if (input != null && rate != null) {
                blockWatcher = true
                it.setAmount(input.multiply(rate), updateType = false, blockWatcher = true)
                blockWatcher = false
            }
        }
    }

    private fun updateFromUpStream() {
        upStreamDependency?.getUntypedValue(false)?.getOrNull()?.let {
            val rate = exchangeRateEdit().getRate(false)
            if (rate != null) {
                setAmount(it.multiply(rate), updateType = false, blockWatcher = true)
            }
        }
    }

    private fun updateFromDownStream() {
        downStreamDependency?.let {
            val amount1 = getAmount(false)
            val amount2 = it.getAmount(false)
            Timber.i("self: %s, downStream: %s", amount1, amount2)
            exchangeRateEdit().calculateAndSetRate(amount1, amount2)
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

    fun setAmount(amount: BigDecimal, updateType: Boolean, blockWatcher: Boolean = false) {
        if (blockWatcher) {
            this.blockWatcher = true
        }
        amountEditText().error = null
        amountEditText().setAmount(amount.abs())
        if (updateType) {
            type = amount.signum() > -1
        }
        this.blockWatcher = false
    }

    fun setRaw(text: String?) {
        amountEditText().setText(text)
    }

    fun clear() {
        amountEditText().setText("")
    }

    fun getUntypedValue(showToUser: Boolean) = amountEditText().getAmount(showToUser)

    val typedValue: BigDecimal
        get() = getTypedValue(showToUser = false).getOrNull() ?: BigDecimal.ZERO

    private fun getTypedValue(showToUser: Boolean) =
        getUntypedValue(showToUser).map {
            if (it != null && !type) it.negate() else it
        }

    fun getAmount(showToUser: Boolean) = getTypedValue(showToUser).getOrNull()

    fun getAmount(
        currencyUnit: CurrencyUnit,
        showToUser: Boolean = true,
    ): Result<Money?> = getTypedValue(showToUser).mapCatching { value ->
        try {
            (value ?: BigDecimal.ZERO.takeIf { !showToUser })?.let { Money(currencyUnit, it) }
        } catch (e: ArithmeticException) {
            if (showToUser) {
                setError("Number too large.")
            }
            throw e
        }
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
                if (selectedCurrency != null) currencyContext[selectedCurrency.code] else null,
                currencyUnit
            )
        }
    }

    fun loadExchangeRate() {
        exchangeRateEdit().loadExchangeRate()
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
            focusedId: Int,
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
