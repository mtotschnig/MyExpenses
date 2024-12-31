package eltos.simpledialogfragment.form

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.google.android.material.textfield.TextInputLayout
import eltos.simpledialogfragment.form.SimpleFormDialog.DialogActions
import eltos.simpledialogfragment.form.SimpleFormDialog.FocusActions
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Money.Companion.convertBigDecimal
import org.totschnig.myexpenses.ui.MyTextWatcher

internal class AmountInputViewHolder(field: AmountInput) : FormElementViewHolder<AmountInput>(field) {
    private lateinit var inputLayout: TextInputLayout
    private lateinit var amountInput: org.totschnig.myexpenses.ui.AmountInput
    override fun getContentViewLayout(): Int {
        return R.layout.simpledialogfragment_form_item_amount
    }

    override fun setUpView(
        view: View,
        context: Context,
        savedInstanceState: Bundle?,
        actions: DialogActions
    ) {
        inputLayout = view.findViewById(R.id.inputLayout)
        amountInput = view.findViewById(R.id.amount)
        inputLayout.hint = field.getText(context)
        amountInput.setFractionDigits(field.fractionDigits)
        field.getText(context)?.let {
            amountInput.contentDescription = it
        }
        field.withTypeSwitch?.also {
            amountInput.setWithTypeSwitch(true)
            amountInput.type = it
        } ?: run {
            amountInput.setWithTypeSwitch(false)
        }
        field.amount?.let {
            amountInput.setAmount(it)
        }
        // Positive button state for single element forms
        if (actions.isOnlyFocusableElement) {
            amountInput.addTextChangedListener(object : MyTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    actions.updatePosButtonState()
                }
            })
        }
    }

    override fun saveState(outState: Bundle) {}
    override fun putResults(results: Bundle, key: String) {
        results.putSerializable(key, amountInput.typedValue)
    }

    override fun focus(actions: FocusActions): Boolean {
        return amountInput.requestFocus()
    }

    override fun posButtonEnabled(context: Context): Boolean {
        return if (!field.required) true else amountInput.getUntypedValue(false).getOrNull() != null
    }

    override fun validate(context: Context): Boolean {
        val result = amountInput.getUntypedValue(true).getOrNull() ?: return false
        try {
            convertBigDecimal(result, field.fractionDigits)
        } catch (e: ArithmeticException) {
            inputLayout.setError("Number too large")
            return false
        }
        if (field.max != null && result > field.max) {
            inputLayout.setError(field.maxExceededError)
            return false
        }
        if (field.min != null && result < field.min) {
            inputLayout.setError(field.underMinError)
            return false
        }
        return true
    }
}
