package org.totschnig.myexpenses.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter.AllCaps
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.common.math.IntMath
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.EditCurrencyBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.form.FormFieldNotEmptyValidator
import org.totschnig.myexpenses.util.form.FormValidator
import org.totschnig.myexpenses.util.form.NumberRangeValidator
import org.totschnig.myexpenses.util.ui.postScrollToBottom
import org.totschnig.myexpenses.util.ui.withOkClick
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel
import java.util.Locale
import kotlin.math.abs

class EditCurrencyDialog : DialogViewBinding<EditCurrencyBinding>() {

    private val editCurrencyViewModel: EditCurrencyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        appComponent.inject(editCurrencyViewModel)
        editCurrencyViewModel.updateComplete.observe(this) { result: Int? -> this.dismiss(result) }
        editCurrencyViewModel.insertComplete.observe(this) { success: Boolean? ->
            if (success != null && success) {
                dismiss()
            } else {
                showSnackBar(R.string.currency_code_already_definded)
                setButtonState(true)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            EditCurrencyBinding.inflate(it)
        }
        val frameworkCurrency: Boolean
        var title: String? = null
        currency?.let {
            binding.edtCurrencySymbol.setText(it.symbol)
            binding.edtCurrencyCode.setText(it.code)
            frameworkCurrency = Utils.isKnownCurrency(it.code)
            if (frameworkCurrency) {
                binding.edtCurrencySymbol.requestFocus()
                title = String.format(Locale.ROOT, "%s (%s)", it.description, it.code)
                binding.containerCurrencyLabel.visibility = View.GONE
                binding.containerCurrencyCode.visibility = View.GONE
            } else {
                binding.edtCurrencyLabel.setText(it.description)
            }
            binding.edtCurrencyFractionDigits.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val newValue = readFractionDigitsFromUI()
                    val oldValue = currentFractionDigits()
                    val valueUpdate = newValue != -1 && newValue != oldValue
                    binding.checkBox.visibility = if (valueUpdate) View.VISIBLE else View.GONE
                    binding.warningChangeFractionDigits.visibility =
                        if (valueUpdate) View.VISIBLE else View.GONE
                    if (valueUpdate) {
                        var message = getString(R.string.warning_change_fraction_digits_1)
                        val delta = oldValue - newValue
                        message += " " + getString(
                            if (delta > 0) R.string.warning_change_fraction_digits_2_multiplied else R.string.warning_change_fraction_digits_2_divided,
                            IntMath.pow(10, abs(delta))
                        )
                        if (delta > 0) {
                            message += " " + getString(R.string.warning_change_fraction_digits_3)
                        }
                        binding.warningChangeFractionDigits.text = message
                        binding.root.postScrollToBottom()
                    }
                }
            })
        } ?: run {
            title = getString(R.string.dialog_title_new_currency)
            with(binding.edtCurrencyCode) {
                isFocusable = true
                isFocusableInTouchMode = true
                isEnabled = true
                filters = arrayOf(AllCaps())
            }
        }
        binding.edtCurrencyFractionDigits.setText(currentFractionDigits().toString())
        return builder
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(title)
            .create()
            .withOkClick { onOkClick() }
    }

    private fun readSymbolFromUI() = binding.edtCurrencySymbol.text.toString()

    private fun readLabelFromUI() = binding.edtCurrencyLabel.text.toString()

    private fun readCodeFromUI() = binding.edtCurrencyCode.text.toString()

    private fun currentFractionDigits() = currency?.fractionDigits ?: 2

    private fun readFractionDigitsFromUI() = try {
        binding.edtCurrencyFractionDigits.text.toString().toInt()
    } catch (_: NumberFormatException) {
        -1
    }

    private val currency: CurrencyUnit?
        get() = BundleCompat.getParcelable(requireArguments(), KEY_CURRENCY, CurrencyUnit::class.java)

    private fun onOkClick() {
        val validator = FormValidator()
        validator.add(FormFieldNotEmptyValidator(binding.edtCurrencySymbol))
        validator.add(NumberRangeValidator(binding.edtCurrencyFractionDigits, 0, 8))
        if (currency == null) {
            validator.add(FormFieldNotEmptyValidator(binding.edtCurrencyCode))
            validator.add(FormFieldNotEmptyValidator(binding.edtCurrencyLabel))
        }
        if (validator.validate()) {
            val withUpdate = binding.checkBox.isChecked
            val label = readLabelFromUI()
            val symbol = readSymbolFromUI()
            val fractionDigits = readFractionDigitsFromUI()
            currency?.code?.let {
                val frameworkCurrency = Utils.isKnownCurrency(it)
                editCurrencyViewModel.save(
                    it,
                    symbol,
                    fractionDigits,
                    if (frameworkCurrency) null else label,
                    withUpdate
                )
                if (!withUpdate && frameworkCurrency) {
                    dismiss()
                } else {
                    setButtonState(false)
                }
            } ?: run {
                editCurrencyViewModel.newCurrency(readCodeFromUI(), symbol, fractionDigits, label)
                setButtonState(false)
            }
        }
    }

    private fun setButtonState(enabled: Boolean) {
        (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = enabled
    }

    fun dismiss(result: Int?) {
        val bundle = Bundle().apply {
            result?.let { putInt(KEY_RESULT, it) }
            currency?.code?.let { putString(KEY_CURRENCY, it) }
        }
        setFragmentResult(REQUEST_KEY, bundle)
        super.dismiss()
    }

    companion object {
        const val REQUEST_KEY = "EditCurrencyDialogRequest"
        const val KEY_RESULT = "result"

        @JvmStatic
        fun newInstance(currency: CurrencyUnit?) = EditCurrencyDialog().apply {
            arguments = Bundle(1).apply {
                putParcelable(KEY_CURRENCY, currency)
            }
        }
    }
}