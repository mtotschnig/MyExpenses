package org.totschnig.myexpenses.fragment

import android.app.ProgressDialog
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
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
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
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ProgressDialogFragment
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class CsvImportDataFragment : Fragment() {
    private var _binding: ImportCsvDataBinding? = null
    private val binding get() = _binding!!
    lateinit var dataSet: ArrayList<CSVRecord>
    lateinit var discardedRows: SparseBooleanArrayParcelable
    private var mFieldAdapter: ArrayAdapter<Int?>? = null
    private var cellParams: LinearLayout.LayoutParams? = null
    private var cbParams: LinearLayout.LayoutParams? = null
    private var firstLineIsHeader = false
    private var nrOfColumns: Int = 0
    private val fieldKeys = arrayOf(
            FIELD_KEY_DISCARD, FIELD_KEY_AMOUNT, FIELD_KEY_EXPENSE, FIELD_KEY_INCOME,
            FIELD_KEY_DATE, FIELD_KEY_PAYEE, FIELD_KEY_COMMENT, FIELD_KEY_CATEGORY,
            FIELD_KEY_SUBCATEGORY, FIELD_KEY_METHOD, FIELD_KEY_STATUS, FIELD_KEY_NUMBER,
            FIELD_KEY_SPLIT
    )
    private val fields = arrayOf(
            R.string.cvs_import_discard,
            R.string.amount,
            R.string.expense,
            R.string.income,
            R.string.date,
            R.string.payer_or_payee,
            R.string.comment,
            R.string.category,
            R.string.subcategory,
            R.string.method,
            R.string.status,
            R.string.reference_number,
            R.string.split_transaction
    )
    private var header2FieldMap: JSONObject? = null
    private var windowWidth = 0f

    @Inject
    lateinit var prefHandler: PrefHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val displayMetrics = resources.displayMetrics
        windowWidth = displayMetrics.widthPixels / displayMetrics.density
        val header2FieldMapJson = prefHandler.getString(PrefKey.CSV_IMPORT_HEADER_TO_FIELD_MAP, null)
        header2FieldMap = if (header2FieldMapJson != null) {
            try {
                JSONObject(header2FieldMapJson)
            } catch (e: JSONException) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
        cbParams = LinearLayout.LayoutParams(UiUtils.dp2Px(CHECKBOX_COLUMN_WIDTH.toFloat(), resources),
                LinearLayout.LayoutParams.WRAP_CONTENT).also {
                    it.setMargins(CELL_MARGIN, CELL_MARGIN, CELL_MARGIN, CELL_MARGIN)
        }
        mFieldAdapter = object : ArrayAdapter<Int?>(
                requireActivity(), android.R.layout.simple_spinner_item, fields) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.text = getString(fields[position])
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.text = getString(fields[position])
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
            setData(savedInstanceState.getSerializable(KEY_DATA_SET) as ArrayList<CSVRecord>?)
            discardedRows = savedInstanceState.getParcelable(KEY_DISCARDED_ROWS)!!
            firstLineIsHeader = savedInstanceState.getBoolean(KEY_FIRST_LINE_IS_HEADER)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setData(data: List<CSVRecord>?) {
        if (data == null || data.isEmpty()) return
        dataSet = data as ArrayList<CSVRecord>
        val nrOfColumns = dataSet[0].size()
        discardedRows = SparseBooleanArrayParcelable()
        val availableCellWidth = ((windowWidth - CHECKBOX_COLUMN_WIDTH - CELL_MARGIN * (nrOfColumns + 2)) / nrOfColumns).toInt()
        val cellWidth: Int
        val tableWidth: Int
        if (availableCellWidth > CELL_MIN_WIDTH) {
            cellWidth = availableCellWidth
            tableWidth = windowWidth.toInt()
        } else {
            cellWidth = CELL_MIN_WIDTH
            tableWidth = CELL_MIN_WIDTH * nrOfColumns + CHECKBOX_COLUMN_WIDTH + CELL_MARGIN * (nrOfColumns + 2)
        }
        cellParams = LinearLayout.LayoutParams(UiUtils.dp2Px(cellWidth.toFloat(), resources),
                LinearLayout.LayoutParams.WRAP_CONTENT)
        cellParams!!.setMargins(CELL_MARGIN, CELL_MARGIN, CELL_MARGIN, CELL_MARGIN)
        with(binding.myRecyclerView) {
            val params = layoutParams
            params.width = UiUtils.dp2Px(tableWidth.toFloat(), resources)
            layoutParams = params
            adapter = MyAdapter()
        }

        //set up header
        with (binding.headerLine) {
            removeViews(1, childCount - 1)
            for (i in 0 until nrOfColumns) {
                val cell = Spinner(activity)
                cell.id = i
                cell.adapter = mFieldAdapter
                addView(cell, cellParams)
            }
        }
    }

    fun setHeader() {
        firstLineIsHeader = true
        binding.myRecyclerView.adapter?.notifyItemChanged(0)
        //autoMap
        val record = dataSet[0]
        outer@ for (j in 0 until record.size()) {
            val headerLabel = Utils.normalize(record[j])
            val keys = header2FieldMap!!.keys()
            while (keys.hasNext()) {
                val storedLabel = keys.next()
                if (storedLabel == headerLabel) {
                    try {
                        val fieldKey = header2FieldMap!!.getString(storedLabel)
                        val position = listOf(*fieldKeys).indexOf(fieldKey)
                        if (position != -1) {
                            (binding.headerLine.getChildAt(j + 1) as Spinner).setSelection(position)
                            continue@outer
                        }
                    } catch (e: JSONException) {
                        //ignore
                    }
                }
            }
            for (i in 1 /* 0=Discard ignored  */ until fields.size) {
                val fieldLabel = Utils.normalize(getString(fields[i]))
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
            Timber.d("%s item at position %d", if (isChecked) "Discarding" else "Including", position)
            if (isChecked) {
                discardedRows.put(position, true)
                if (position == 0) {
                    val b = Bundle()
                    b.putInt(ConfirmationDialogFragment.KEY_TITLE,
                            R.string.dialog_title_information)
                    b.putString(
                            ConfirmationDialogFragment.KEY_MESSAGE,
                            getString(R.string.cvs_import_set_first_line_as_header))
                    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                            R.id.SET_HEADER_COMMAND)
                    ConfirmationDialogFragment.newInstance(b).show(
                            parentFragmentManager, "SET_HEADER_CONFIRMATION")
                }
            } else {
                discardedRows.delete(position)
                if (position == 0) {
                    firstLineIsHeader = false
                }
            }
            notifyItemChanged(position)
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        inner class ViewHolder(  // each data item is just a string in this case
                var row: LinearLayout) : RecyclerView.ViewHolder(row)

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): ViewHolder {
            // create a new view
            val v = LinearLayout(parent.context)
            v.setBackgroundResource(R.drawable.csv_import_row_background)
            var cell: TextView = CheckBox(parent.context)
            v.addView(cell, cbParams)
            for (i in 0 until nrOfColumns) {
                cell = TextView(parent.context)
                cell.setSingleLine()
                cell.ellipsize = TextUtils.TruncateAt.END
                cell.isSelected = true
                cell.setOnClickListener { v1: View -> (activity as ProtectedFragmentActivity?)!!.showSnackbar((v1 as TextView).text) }
                if (viewType == 0) {
                    cell.setTypeface(null, Typeface.BOLD)
                }
                v.addView(cell, cellParams)
            }
            // set the view's size, margins, paddings and layout parameters
            return ViewHolder(v)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // - get element from your dataSet at this position
            // - replace the contents of the view with that element
            val isDiscarded = discardedRows[position, false]
            val isHeader = position == 0 && firstLineIsHeader
            holder.row.isActivated = isDiscarded && !isHeader
            val record = dataSet[position]
            var i = 0
            while (i < record.size() && i < nrOfColumns) {
                val cell = holder.row.getChildAt(i + 1) as TextView
                cell.text = record[i]
                i++
            }
            val cb = holder.row.getChildAt(0) as CheckBox
            cb.tag = position
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = isDiscarded
            cb.setOnCheckedChangeListener(this)
        }

        // Return the size of your dataSet (invoked by the layout manager)
        override fun getItemCount(): Int {
            return dataSet.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0 && firstLineIsHeader) 0 else 1
        }

        // Provide a suitable constructor (depends on the kind of dataSet)
        init {
            nrOfColumns = dataSet[0].size()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_DATA_SET, dataSet)
        outState.putParcelable(KEY_DISCARDED_ROWS, discardedRows)
        outState.putBoolean(KEY_FIRST_LINE_IS_HEADER, firstLineIsHeader)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.csv_import, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.IMPORT_COMMAND) {
            val columnToFieldMap = IntArray(nrOfColumns)
            val header = dataSet[0]
            for (i in 0 until nrOfColumns) {
                val position = (binding.headerLine.getChildAt(i + 1) as Spinner).selectedItemPosition
                columnToFieldMap[i] = fields[position]
                if (firstLineIsHeader) {
                    try {
                        if (fieldKeys[position] != FIELD_KEY_DISCARD) {
                            header2FieldMap!!.put(Utils.normalize(header[i]), fieldKeys[position])
                        }
                    } catch (e: JSONException) {
                        CrashHandler.report(e)
                    }
                }
            }
            if (validateMapping(columnToFieldMap)) {
                prefHandler.putString(PrefKey.CSV_IMPORT_HEADER_TO_FIELD_MAP, header2FieldMap.toString())
                (activity as? CsvImportActivity)?.importData(dataSet, columnToFieldMap, discardedRows)
                /*val taskExecutionFragment = TaskExecutionFragment.newInstanceCSVImport(
                        dataSet, columnToFieldMap, discardedRows, format, accountId, currency, type)
                val progressDialogFragment = ProgressDialogFragment.newInstance(
                        getString(R.string.pref_import_title, "CSV"),
                        null, ProgressDialog.STYLE_HORIZONTAL, false)
                progressDialogFragment.max = dataSet.size - discardedRows.size()
                parentFragmentManager
                        .beginTransaction()
                        .add(taskExecutionFragment, ProtectedFragmentActivity.ASYNC_TAG)
                        .add(progressDialogFragment, ProtectedFragmentActivity.PROGRESS_TAG)
                        .commit()*/
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
     *
     * @param columnToFieldMap
     */
    private fun validateMapping(columnToFieldMap: IntArray): Boolean {
        val foundFields = SparseBooleanArray()
        val activity = activity as ProtectedFragmentActivity?
        for (field in columnToFieldMap) {
            if (field != R.string.cvs_import_discard) {
                if (foundFields[field, false]) {
                    activity!!.showSnackbar(getString(R.string.csv_import_field_mapped_more_than_once, getString(field)))
                    return false
                }
                foundFields.put(field, true)
            }
        }
        if (foundFields[R.string.subcategory, false] && !foundFields[R.string.category, false]) {
            activity!!.showSnackbar(R.string.csv_import_subcategory_requires_category)
            return false
        }
        if (!(foundFields[R.string.amount, false] ||
                        foundFields[R.string.expense, false] ||
                        foundFields[R.string.income, false])) {
            activity!!.showSnackbar(R.string.csv_import_no_mapping_found_for_amount)
            return false
        }
        return true
    }

    companion object {
        const val KEY_DATA_SET = "DATA_SET"
        const val KEY_DISCARDED_ROWS = "DISCARDED_ROWS"
        const val KEY_FIELD_TO_COLUMN = "FIELD_TO_COLUMN"
        const val KEY_FIRST_LINE_IS_HEADER = "FIRST_LINE_IS_HEADER"
        const val CELL_MIN_WIDTH = 100
        const val CHECKBOX_COLUMN_WIDTH = 60
        const val CELL_MARGIN = 5
        const val FIELD_KEY_DISCARD = "DISCARD"
        const val FIELD_KEY_AMOUNT = "AMOUNT"
        const val FIELD_KEY_EXPENSE = "EXPENSE"
        const val FIELD_KEY_INCOME = "INCOME"
        const val FIELD_KEY_DATE = "DATE"
        const val FIELD_KEY_PAYEE = "PAYEE"
        const val FIELD_KEY_COMMENT = "COMMENT"
        const val FIELD_KEY_CATEGORY = "CATEGORY"
        const val FIELD_KEY_SUBCATEGORY = "SUB_CATEGORY"
        const val FIELD_KEY_METHOD = "METHOD"
        const val FIELD_KEY_STATUS = "STATUS"
        const val FIELD_KEY_NUMBER = "NUMBER"
        const val FIELD_KEY_SPLIT = "SPLIT"
        fun newInstance(): CsvImportDataFragment {
            return CsvImportDataFragment()
        }
    }
}