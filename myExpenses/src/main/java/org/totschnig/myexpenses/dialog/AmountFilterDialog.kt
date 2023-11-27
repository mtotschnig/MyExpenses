package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.databinding.FilterAmountBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.AmountCriterion.Companion.create
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.ui.validateAmountInput
import org.totschnig.myexpenses.util.ui.withOkClick

class AmountFilterDialog : DialogViewBinding<FilterAmountBinding>() {

    val currency: CurrencyUnit
        get() = (requireArguments().getSerializable(DatabaseConstants.KEY_CURRENCY) as CurrencyUnit)

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
        (binding.Operator.adapter as ArrayAdapter<*>)
            .setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        val fractionDigits = currency.fractionDigits
        binding.amount1.fractionDigits = fractionDigits
        binding.amount2.fractionDigits = fractionDigits
        return builder
            .setTitle(R.string.search_amount)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create().withOkClick(::onOkClick)
    }

    private fun onOkClick() {
        val ctx = activity as MyExpenses? ?: return
        val currency = this.currency
        val amount1 = binding.amount1.validateAmountInput(currency) ?: return
        val selectedOp =
            resources.getStringArray(R.array.comparison_operator_values)[binding.Operator.selectedItemPosition]
        val type = binding.type.checkedButtonId == R.id.income
        val amount2 = if (selectedOp == "BTW") {
            binding.amount2.validateAmountInput(currency)
        } else null

        ctx.addFilterCriterion(
            create(
                WhereFilter.Operation.valueOf(selectedOp),
                currency.code,
                type,
                amount1.amountMinor,
                amount2?.amountMinor
            )
        )
        dismiss()
    }

    companion object {
        fun newInstance(currency: CurrencyUnit?) = AmountFilterDialog().apply {
            arguments = Bundle().apply {
                putSerializable(DatabaseConstants.KEY_CURRENCY, currency)
            }
        }
    }
}