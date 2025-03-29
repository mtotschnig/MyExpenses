package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.R.layout.support_simple_spinner_dropdown_item
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.FilterAmountBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.AmountCriterion
import org.totschnig.myexpenses.provider.filter.AmountCriterion.Companion.create
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.filter.criterion
import org.totschnig.myexpenses.util.ui.withOkClick

class AmountFilterDialog : DialogViewBinding<FilterAmountBinding>() {

    val currency: CurrencyUnit by lazy {
        BundleCompat.getSerializable(requireArguments(), DatabaseConstants.KEY_CURRENCY,
            CurrencyUnit::class.java)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            FilterAmountBinding.inflate(it)
        }

        binding.Operator.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?,
                position: Int, id: Long
            ) {
                binding.amount1.contentDescription =
                    resources.getStringArray(R.array.comparison_operator_entries)[position]
                binding.Amount2Row.isVisible =
                    (resources.getStringArray(R.array.comparison_operator_values)[position] == "BTW")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        (binding.Operator.adapter as ArrayAdapter<*>).setDropDownViewResource(
            support_simple_spinner_dropdown_item
        )
        val fractionDigits = currency.fractionDigits
        binding.amount1.fractionDigits = fractionDigits
        binding.amount2.fractionDigits = fractionDigits
        requireArguments().criterion(AmountCriterion::class.java)?.let { criterion ->
            if (criterion.sign) {
                binding.type.check(R.id.income)
            }
            val transformed = criterion.transformForUi()
            binding.Operator.setSelection(operations.indexOf(transformed.first.name))
            binding.amount1.setAmount(Money(currency, transformed.second[0]).amountMajor)
            transformed.second.getOrNull(1)?.let {
                binding.amount2.setAmount(Money(currency, it).amountMajor)
            }
        }
        return builder
            .setTitle(R.string.search_amount)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .withOkClick(::onOkClick)
    }

    private val operations
        get() = resources.getStringArray(R.array.comparison_operator_values)

    private fun onOkClick() {
        val currency = this.currency
        val amount1 = binding.amount1.getAmount(currency).getOrNull() ?: return
        val selectedOp = operations[binding.Operator.selectedItemPosition]
        val type = binding.type.checkedButtonId == R.id.income
        val amount2 = if (selectedOp == "BTW") {
            binding.amount2.getAmount(currency).getOrNull() ?: return
        } else null
        parentFragmentManager.confirmFilter(
            requestKey,
            create(
                Operation.valueOf(selectedOp),
                currency.code,
                type,
                amount1.amountMinor,
                amount2?.amountMinor
            )
        )
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.confirmFilter(requestKey, null)
    }

    companion object {

        fun newInstance(requestKey: String, currency: CurrencyUnit?, amountCriterion: AmountCriterion?) =
            AmountFilterDialog().apply {
                arguments = configureArguments(requestKey).apply {
                    putSerializable(DatabaseConstants.KEY_CURRENCY, currency)
                    putParcelable(KEY_CRITERION, amountCriterion)
                }
            }
    }
}