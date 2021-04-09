package org.totschnig.myexpenses.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.apache.commons.csv.CSVRecord
import org.json.JSONException
import org.json.JSONObject
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.ImportCsvDataBinding
import org.totschnig.myexpenses.databinding.ImportCsvDataRowBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class CsvImportDataFragment : Fragment() {
    private var _binding: ImportCsvDataBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataSet: ArrayList<CSVRecord>
    private lateinit var selectedRows: SparseBooleanArrayParcelable
    private lateinit var mFieldAdapter: ArrayAdapter<Pair<Int, String?>>
    private lateinit var cellParams: LinearLayout.LayoutParams
    private var headerLine = -1
    private var nrOfColumns: Int = 0
    private val allFields: List<Pair<Int, String?>> = listOf(
            R.string.discard to null,
            R.string.amount to "AMOUNT",
            R.string.expense to "EXPENSE",
            R.string.income to "INCOME",
            R.string.date to "DATE",
            R.string.booking_date to "BOOKING_DATE",
            R.string.value_date to "VALUE_DATE",
            R.string.payer_or_payee to "PAYEE",
            R.string.comment to "COMMENT",
            R.string.category to "CATEGORY",
            R.string.subcategory to "SUB_CATEGORY",
            R.string.method to "METHOD",
            R.string.status to "STATUS",
            R.string.reference_number to "NUMBER",
            R.string.split_transaction to "SPLIT"
    )

    private lateinit var fields: List<Pair<Int, String?>>
    private lateinit var header2FieldMap: JSONObject
    private var windowWidth = 0
    private var cellMinWidth = 0
    private var checkboxColumnWidth = 0
    private var cellMargin = 0
    private var spinnerRightPadding = 0

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
        setHasOptionsMenu(true)
        cellMinWidth = resources.getDimensionPixelSize(R.dimen.csv_import_cell_min_width)
        checkboxColumnWidth = resources.getDimensionPixelSize(R.dimen.csv_import_checkbox_column_width)
        cellMargin = resources.getDimensionPixelSize(R.dimen.csv_import_cell_margin)
        spinnerRightPadding = resources.getDimensionPixelSize(R.dimen.csv_import_spinner_right_padding)
        val withValueDate = prefHandler.getBoolean(PrefKey.TRANSACTION_WITH_VALUE_DATE, false)
        fields = allFields.filter {
            when (it.first) {
                R.string.date -> !withValueDate
                R.string.booking_date, R.string.value_date -> withValueDate
                else -> true
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val displayMetrics = resources.displayMetrics
        windowWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        header2FieldMap = prefHandler.getString(PrefKey.CSV_IMPORT_HEADER_TO_FIELD_MAP, null)?.let {
            try {
                JSONObject(it)
            } catch (e: JSONException) {
                null
            }
        } ?: JSONObject()
        mFieldAdapter = object : ArrayAdapter<Pair<Int, String?>>(
                requireActivity(), R.layout.spinner_item_narrow, 0, fields) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.text = getString(fields[position].first)
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.text = getString(fields[position].first)
                return tv
            }
        }.also {
            it.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        }
        _binding = ImportCsvDataBinding.inflate(inflater, container, false)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // http://www.vogella.com/tutorials/AndroidRecyclerView/article.html
        binding.myRecyclerView.setHasFixedSize(true)

        if (savedInstanceState != null) {
            setData(savedInstanceState.getSerializable(KEY_DATA_SET) as? ArrayList<CSVRecord>)
            selectedRows = savedInstanceState.getParcelable(KEY_SELECTED_ROWS)!!
            headerLine = savedInstanceState.getInt(KEY_HEADER_LINE_POSITION)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setData(data: List<CSVRecord>?) {
        if (data == null || data.isEmpty()) return
        dataSet = ArrayList(data)
        nrOfColumns = dataSet.map { it.size() }.maxOrNull()!!
        selectedRows = SparseBooleanArrayParcelable()
        for (i in 0 until dataSet.size) {
            selectedRows.put(i, true)
        }
        val availableCellWidth = ((windowWidth - checkboxColumnWidth - cellMargin * nrOfColumns * 2) / nrOfColumns)
        val cellWidth: Int
        val tableWidth: Int
        if (availableCellWidth > cellMinWidth) {
            cellWidth = availableCellWidth
            tableWidth = windowWidth
        } else {
            cellWidth = cellMinWidth
            tableWidth = cellMinWidth * nrOfColumns + checkboxColumnWidth + cellMargin * nrOfColumns * 2
        }
        cellParams = LinearLayout.LayoutParams(cellWidth, MATCH_PARENT).apply {
            setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
        }
        with(binding.myRecyclerView) {
            val params = layoutParams
            params.width = tableWidth
            layoutParams = params
            adapter = MyAdapter()
        }

        //set up header
        with(binding.headerLine) {
            removeViews(1, childCount - 1)
            for (i in 0 until nrOfColumns) {
                val cell = AppCompatSpinner(requireContext())
                cell.id = ViewCompat.generateViewId()
                cell.adapter = mFieldAdapter
                ViewCompat.setPaddingRelative(cell, 0, 0, spinnerRightPadding, 0)
                addView(cell, cellParams)
            }
        }
    }

    fun setHeader(rowPosition: Int) {
        headerLine = rowPosition
        binding.myRecyclerView.adapter?.notifyItemChanged(rowPosition)
        //autoMap
        val record = dataSet[rowPosition]
        outer@ for (j in 0 until record.size()) {
            val headerLabel = Utils.normalize(record[j])
            val keys = header2FieldMap.keys()
            while (keys.hasNext()) {
                val storedLabel = keys.next()
                if (storedLabel == headerLabel) {
                    try {
                        val fieldKey = header2FieldMap.getString(storedLabel)
                        val fieldPosition = fields.indexOfFirst { it.second == fieldKey }
                        if (fieldPosition != -1) {
                            (binding.headerLine.getChildAt(j + 1) as Spinner).setSelection(fieldPosition)
                            continue@outer
                        }
                    } catch (e: JSONException) {
                        //ignore
                    }
                }
            }
            for (i in 1 /* 0=Select ignored  */ until fields.size) {
                val fieldLabel = Utils.normalize(getString(fields[i].first))
                if (fieldLabel == headerLabel) {
                    (binding.headerLine.getChildAt(j + 1) as Spinner).setSelection(i)
                    break
                }
            }
        }
    }

    private inner class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>(), CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            val position = buttonView.tag as Int
            Timber.d("%s item at position %d", if (isChecked) "Selecting" else "Discarding", position)
            if (isChecked) {
                selectedRows.put(position, true)
                if (position == headerLine) {
                    headerLine = -1
                }
            } else {
                if (position == firstSelectedRow()) {
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putInt(ConfirmationDialogFragment.KEY_TITLE,
                                R.string.dialog_title_information)
                        putString(
                                ConfirmationDialogFragment.KEY_MESSAGE,
                                getString(R.string.cvs_import_set_first_line_as_header))
                        putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                                R.id.SET_HEADER_COMMAND)
                        putInt(KEY_HEADER_LINE_POSITION, position)
                        putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.response_yes)
                        putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, R.string.response_no)
                    }).show(
                            parentFragmentManager, "SET_HEADER_CONFIRMATION")
                }
                selectedRows.delete(position)
            }
            notifyItemChanged(position)
        }

        inner class ViewHolder(val itemBinding: ImportCsvDataRowBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): ViewHolder {
            val itemBinding = ImportCsvDataRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            for (i in 0 until nrOfColumns) {
                val cell = TextView(parent.context)
                cell.setSingleLine()
                cell.ellipsize = TextUtils.TruncateAt.END
                cell.isSelected = true
                cell.setOnClickListener { v1: View -> (requireActivity() as ProtectedFragmentActivity).showSnackbar((v1 as TextView).text) }
                if (viewType == 0) {
                    cell.setTypeface(null, Typeface.BOLD)
                }
                itemBinding.root.addView(cell, cellParams)
            }
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val isSelected = selectedRows[position, false]
            val isHeader = position == headerLine
            holder.itemView.isActivated = !isSelected && !isHeader
            val record = dataSet[position]
            for (i in 0 until record.size()) {
                val cell = holder.itemBinding.root.getChildAt(i + 1) as TextView
                cell.text = record[i]
            }
            with(holder.itemBinding.checkBox) {
                tag = position
                setOnCheckedChangeListener(null)
                isChecked = isSelected
                setOnCheckedChangeListener(this@MyAdapter)
            }
        }

        // Return the size of your dataSet (invoked by the layout manager)
        override fun getItemCount() = dataSet.size

        override fun getItemViewType(position: Int) = if (position == headerLine) 0 else 1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_DATA_SET, dataSet)
        outState.putParcelable(KEY_SELECTED_ROWS, selectedRows)
        outState.putInt(KEY_HEADER_LINE_POSITION, headerLine)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.csv_import, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.IMPORT_COMMAND) {
            val columnToFieldMap = IntArray(nrOfColumns)
            val header = headerLine.takeIf { it > -1 }?.let { dataSet[it] }
            for (i in 0 until nrOfColumns) {
                val position = (binding.headerLine.getChildAt(i + 1) as Spinner).selectedItemPosition
                columnToFieldMap[i] = fields[position].first
                if (position > 0) {
                    header?.let {
                        try {
                            header2FieldMap.put(Utils.normalize(header[i]), fields[position].second)
                        } catch (e: JSONException) {
                            CrashHandler.report(e)
                        }
                    }
                }
            }
            if (validateMapping(columnToFieldMap)) {
                prefHandler.putString(PrefKey.CSV_IMPORT_HEADER_TO_FIELD_MAP, header2FieldMap.toString())
                val selectedData = dataSet.filterIndexed { index, _ -> selectedRows[index] }
                (activity as? CsvImportActivity)?.importData(selectedData, columnToFieldMap, dataSet.size - selectedData.size)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Check mapping constraints:<br></br>
     *
     *  * No field mapped more than once
     *  * Subcategory cannot be mapped without category
     *  * One of amount, income or expense must be mapped.
     *
     * @param columnToFieldMap
     */
    private fun validateMapping(columnToFieldMap: IntArray): Boolean {
        val foundFields = SparseBooleanArray()
        val activity = requireActivity() as ProtectedFragmentActivity
        for (field in columnToFieldMap) {
            if (field != R.string.discard) {
                if (foundFields[field, false]) {
                    activity.showSnackbar(getString(R.string.csv_import_field_mapped_more_than_once, getString(field)))
                    return false
                }
                foundFields.put(field, true)
            }
        }
        if (foundFields[R.string.subcategory, false] && !foundFields[R.string.category, false]) {
            activity.showSnackbar(R.string.csv_import_subcategory_requires_category)
            return false
        }
        if (!(foundFields[R.string.amount, false] ||
                        foundFields[R.string.expense, false] ||
                        foundFields[R.string.income, false])) {
            activity.showSnackbar(R.string.csv_import_no_mapping_found_for_amount)
            return false
        }
        return true
    }

    private fun firstSelectedRow(): Int {
        for (i in 0 until dataSet.size) {
            if (selectedRows.get(i)) return i
        }
        return -1
    }

    companion object {
        const val KEY_DATA_SET = "DATA_SET"
        const val KEY_SELECTED_ROWS = "SELECTED_ROWS"
        const val KEY_HEADER_LINE_POSITION = "HEADER_LINE_POSITION"
        fun newInstance(): CsvImportDataFragment {
            return CsvImportDataFragment()
        }
    }
}