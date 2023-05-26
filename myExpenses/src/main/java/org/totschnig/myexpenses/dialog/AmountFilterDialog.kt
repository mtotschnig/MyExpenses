package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyExpenses
import org.totschnig.myexpenses.databinding.FilterAmountBinding
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.AmountCriterion.Companion.create
import org.totschnig.myexpenses.provider.filter.WhereFilter
import java.math.BigDecimal

class AmountFilterDialog : DialogViewBinding<FilterAmountBinding>(),
    DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            FilterAmountBinding.inflate(it)
        }

        binding.Operator.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View,
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
        val fractionDigits =
            (requireArguments().getSerializable(DatabaseConstants.KEY_CURRENCY) as CurrencyUnit?)!!.fractionDigits
        binding.amount1.fractionDigits = fractionDigits
        binding.amount2.fractionDigits = fractionDigits
        return builder
            .setTitle(R.string.search_amount)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val ctx = activity as MyExpenses? ?: return
        val bdAmount1 = binding.amount1.validate(false) ?: return
        var bdAmount2: BigDecimal? = null
        val selectedOp =
            resources.getStringArray(R.array.comparison_operator_values)[binding.Operator.selectedItemPosition]
        val type = binding.type.checkedButtonId == R.id.income
        if (selectedOp == "BTW") {
            bdAmount2 = binding.amount2.validate(false)
            if (bdAmount2 == null) {
                return
            }
        }
        val currency =
            requireArguments().getSerializable(DatabaseConstants.KEY_CURRENCY) as CurrencyUnit?
        ctx.addFilterCriterion(
            create(
                WhereFilter.Operation.valueOf(selectedOp),
                currency!!.code,
                type,
                Money(currency, bdAmount1).amountMinor,
                if (bdAmount2 != null) Money(currency, bdAmount2).amountMinor else null
            )
        )
    }

    companion object {
        fun newInstance(currency: CurrencyUnit?) = AmountFilterDialog().apply {
            arguments = Bundle().apply {
                putSerializable(DatabaseConstants.KEY_CURRENCY, currency)
            }
        }
    }
}