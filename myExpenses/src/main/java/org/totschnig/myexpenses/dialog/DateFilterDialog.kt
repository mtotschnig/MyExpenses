package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import androidx.core.view.isVisible
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.FilterDateBinding
import org.totschnig.myexpenses.provider.filter.DateCriterion
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.filter.criterion
import java.time.LocalDate

class DateFilterDialog : DialogViewBinding<FilterDateBinding>(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder {
            FilterDateBinding.inflate(it)
        }
        binding.Operator.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?,
                position: Int, id: Long
            ) {
                val isRange =
                    resources.getStringArray(R.array.comparison_operator_date_values)[position] == "BTW"
                binding.Date2And.isVisible = isRange
                binding.date2.isVisible = isRange
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        (binding.Operator.adapter as ArrayAdapter<*>)
            .setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)

        requireArguments().criterion(DateCriterion::class.java)?.let { criterion ->
            binding.Operator.setSelection(operations.indexOf(criterion.operation.name))
            binding.date1.selectedDate = criterion.values[0]
            criterion.values.getOrNull(1)?.let {
                binding.date2.selectedDate = it
            }
        }

        return builder
            .setTitle(R.string.search_date)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private var DatePicker.selectedDate
        get() = LocalDate.of(year, month + 1, dayOfMonth)
        set(value) {
            updateDate(value.year, value.monthValue - 1, value.dayOfMonth)
        }

    private val operations
        get() = resources.getStringArray(R.array.comparison_operator_date_values)

    override fun onClick(dialog: DialogInterface, which: Int) {
        val selectedOp = operations[binding.Operator.selectedItemPosition]
        val date1: LocalDate = binding.date1.selectedDate
        parentFragmentManager.confirmFilter(requestKey,
            if (selectedOp == "BTW") {
                DateCriterion(date1, binding.date2.selectedDate)
            } else {
                DateCriterion(
                    Operation.valueOf(selectedOp),
                    date1
                )
            }
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.confirmFilter(requestKey, null)
    }

    companion object {
        fun newInstance(requestKey: String, dateCriterion: DateCriterion?) = DateFilterDialog().apply {
            arguments = configureArguments(requestKey).apply {
                putParcelable(KEY_CRITERION, dateCriterion)
            }
        }
    }
}
