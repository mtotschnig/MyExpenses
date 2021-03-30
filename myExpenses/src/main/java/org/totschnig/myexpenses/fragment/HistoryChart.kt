package org.totschnig.myexpenses.fragment

import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ViewPortHandler
import icepick.Icepick
import icepick.State
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.temporal.JulianFields
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.databinding.HistoryChartBinding
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment.Companion.newInstance
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.ui.ExactStackedBarHighlighter
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.viewmodel.HistoryViewModel
import org.totschnig.myexpenses.viewmodel.data.HistoryAccountInfo
import java.text.DateFormat
import java.util.*
import javax.inject.Inject

class HistoryChart : Fragment(), LoaderManager.LoaderCallbacks<Cursor?> {
    private var _binding: HistoryChartBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var accountInfo: HistoryAccountInfo

    @JvmField
    @State
    var grouping: Grouping? = null
    private val filter = WhereFilter.empty()
    private var valueTextSize = 10f

    @ColorInt
    private var textColor = Color.WHITE

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var prefHandler: PrefHandler

    @JvmField
    @State
    var showBalance = true

    @JvmField
    @State
    var includeTransfers = false

    @JvmField
    @State
    var showTotals = true

    val viewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
        setHasOptionsMenu(true)
        if (savedInstanceState == null) {
            grouping =  requireActivity().intent.getStringExtra(DatabaseConstants.KEY_GROUPING)?.let { Grouping.valueOf(it) }?.takeIf { it != Grouping.NONE }  ?: Grouping.MONTH
        } else {
            Icepick.restoreInstanceState(this, savedInstanceState)
        }
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(android.R.attr.textAppearanceSmall, typedValue, true)
        val textSizeAttr = intArrayOf(android.R.attr.textSize)
        val indexOfAttrTextSize = 0
        val a = requireActivity().obtainStyledAttributes(typedValue.data, textSizeAttr)
        valueTextSize = a.getDimensionPixelSize(indexOfAttrTextSize, 10) / resources.displayMetrics.density
        a.recycle()
        textColor = UiUtils.getColor(requireContext(), R.attr.colorOnSurface)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.account(requireActivity().intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0)).observe(viewLifecycleOwner) {
            accountInfo = HistoryAccountInfo(it.id, it.getLabelForScreenTitle(requireActivity()), it.currencyUnit, it.color, it.openingBalance)
            (requireActivity() as ProtectedFragmentActivity).supportActionBar?.title = accountInfo.label
            LoaderManager.getInstance(this).initLoader(GROUPING_CURSOR, null, this)
        }
        showBalance = prefHandler.getBoolean(PrefKey.HISTORY_SHOW_BALANCE, showBalance)
        includeTransfers = prefHandler.getBoolean(PrefKey.HISTORY_INCLUDE_TRANSFERS, includeTransfers)
        showTotals = prefHandler.getBoolean(PrefKey.HISTORY_SHOW_TOTALS, showTotals)
        _binding = HistoryChartBinding.inflate(inflater, container, false)
        with(binding.historyChart) {
            description.isEnabled = false
            with(xAxis) {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IAxisValueFormatter { value: Float, axis: AxisBase -> if (axis.axisMinimum == value) "" else formatXValue(value) }
                textColor = textColor
            }
            configureYAxis(axisLeft)
            configureYAxis(axisRight)
            legend.textColor = textColor
            isHighlightPerDragEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, h: Highlight) {
                    if (h.stackIndex > -1) {
                        //expense is first entry, income second
                        val type = if (h.stackIndex == 0) -1 else 1
                        newInstance(
                                accountInfo.id, 0, false, grouping, buildGroupingClause(e.x.toInt()), null, formatXValue(e.x), type, includeTransfers)
                                .show(parentFragmentManager, TransactionListDialogFragment::class.java.name)
                    }
                }

                override fun onNothingSelected() {}
            })
        }
        return binding.root
    }

    private fun formatXValue(value: Float): String = when (grouping) {
        Grouping.DAY -> LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, value.toLong())
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        Grouping.WEEK -> LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, julianDayFromWeekNumber(value))
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        Grouping.MONTH -> Grouping.getDisplayTitleForMonth((value / MONTH_GROUPING_YEAR_X).toInt(), (value % MONTH_GROUPING_YEAR_X).toInt(), DateFormat.SHORT,
                userLocaleProvider.getUserPreferredLocale())
        Grouping.YEAR -> String.format(Locale.ROOT, "%d", value.toInt())
        else -> ""
    }

    private fun julianDayFromWeekNumber(value: Float): Long {
        return (value * 7).toLong() + JULIAN_DAY_WEEK_OFFSET
    }

    private fun buildGroupingClause(x: Int): String? = when (grouping) {
        Grouping.DAY -> DatabaseConstants.DAY_START_JULIAN + " = " + x
        Grouping.WEEK -> DatabaseConstants.getWeekStartJulian() + " = " + julianDayFromWeekNumber(x.toFloat())
        Grouping.MONTH -> DatabaseConstants.getYearOfMonthStart() + " = " + x / MONTH_GROUPING_YEAR_X + " AND " + DatabaseConstants.getMonth() + " = " + x % MONTH_GROUPING_YEAR_X
        Grouping.YEAR -> DatabaseConstants.YEAR + " = " + x
        else -> null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.grouping, menu)
        inflater.inflate(R.menu.history, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val subMenu = menu.findItem(R.id.GROUPING_COMMAND).subMenu
        subMenu.findItem(R.id.GROUPING_NONE_COMMAND).isVisible = false
        Utils.configureGroupingMenu(subMenu, grouping)
        var m = menu.findItem(R.id.TOGGLE_BALANCE_COMMAND)
        if (m != null) {
            m.isChecked = showBalance
        }
        m = menu.findItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND)
        if (m != null) {
            m.isChecked = includeTransfers
        }
        m = menu.findItem(R.id.TOGGLE_TOTALS_COMMAND)
        if (m != null) {
            m.isChecked = showTotals
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (handleGrouping(item)) return true
        when (item.itemId) {
            R.id.TOGGLE_BALANCE_COMMAND -> {
                showBalance = !showBalance
                prefHandler.putBoolean(PrefKey.HISTORY_SHOW_BALANCE, showBalance)
                reset()
                return true
            }
            R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND -> {
                includeTransfers = !includeTransfers
                prefHandler.putBoolean(PrefKey.HISTORY_INCLUDE_TRANSFERS, includeTransfers)
                reset()
                return true
            }
            R.id.TOGGLE_TOTALS_COMMAND -> {
                showTotals = !showTotals
                prefHandler.putBoolean(PrefKey.HISTORY_SHOW_TOTALS, showTotals)
                reset()
                return true
            }
            else -> return false
        }
    }

    private fun reset() {
        binding.historyChart.clear()
        LoaderManager.getInstance(this).restartLoader(GROUPING_CURSOR, null, this)
    }

    private fun handleGrouping(item: MenuItem): Boolean {
        val newGrouping = Utils.getGroupingFromMenuItemId(item.itemId)
        if (newGrouping != null) {
            if (!item.isChecked) {
                grouping = newGrouping
                requireActivity().invalidateOptionsMenu()
                reset()
            }
            return true
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        if (id == GROUPING_CURSOR) {
            var selection: String? = null
            var selectionArgs: Array<String?>? = null
            val builder = TransactionProvider.TRANSACTIONS_URI.buildUpon()
            //TODO enable filtering ?
            if (!filter.isEmpty) {
                selection = filter.getSelectionForParts(DatabaseConstants.VIEW_EXTENDED) //GROUP query uses extended view
                if (selection != "") {
                    selectionArgs = filter.getSelectionArgs(true)
                }
            }
            builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
                    .appendPath(grouping!!.name)
            if (!Account.isHomeAggregate(accountInfo.id)) {
                if (Account.isAggregate(accountInfo.id)) {
                    builder.appendQueryParameter(DatabaseConstants.KEY_CURRENCY, accountInfo.currency.code)
                } else {
                    builder.appendQueryParameter(DatabaseConstants.KEY_ACCOUNTID, accountInfo.id.toString())
                }
            }
            if (shouldUseGroupStart()) {
                builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_START, "1")
            }
            if (includeTransfers) {
                builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_INCLUDE_TRANSFERS, "1")
            }
            return CursorLoader(requireActivity(),
                    builder.build(),
                    null, selection, selectionArgs, null)
        }
        throw IllegalArgumentException()
    }

    private fun shouldUseGroupStart(): Boolean {
        return grouping == Grouping.WEEK || grouping == Grouping.DAY
    }

    private fun calculateX(year: Int, second: Int, groupStart: Int): Int = when (grouping) {
        Grouping.DAY -> groupStart
        Grouping.WEEK -> groupStart / 7
        Grouping.MONTH -> year * MONTH_GROUPING_YEAR_X + second
        Grouping.YEAR -> year
        else -> 0
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, cursor: Cursor?) {
        val context = activity as ProtectedFragmentActivity? ?: return
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndexGroupSumIncome = cursor.getColumnIndex(DatabaseConstants.KEY_SUM_INCOME)
            val columnIndexGroupSumExpense = cursor.getColumnIndex(DatabaseConstants.KEY_SUM_EXPENSES)
            val columnIndexGroupSumTransfer = cursor.getColumnIndex(DatabaseConstants.KEY_SUM_TRANSFERS)
            val columnIndexGroupYear = cursor.getColumnIndex(DatabaseConstants.KEY_YEAR)
            val columnIndexGroupSecond = cursor.getColumnIndex(DatabaseConstants.KEY_SECOND_GROUP)
            val columnIndexGroupStart = cursor.getColumnIndex(DatabaseConstants.KEY_GROUP_START)
            val barEntries = ArrayList<BarEntry>()
            val lineEntries = ArrayList<Entry>()
            val xAxis = binding.historyChart.xAxis
            var previousBalance = accountInfo.openingBalance.amountMinor
            var interimBalance = 0L
            do {
                val sumIncome = cursor.getLong(columnIndexGroupSumIncome)
                val sumExpense = cursor.getLong(columnIndexGroupSumExpense)
                val sumTransfer = if (columnIndexGroupSumTransfer > -1) cursor.getLong(columnIndexGroupSumTransfer) else 0
                val delta = sumIncome + sumExpense + sumTransfer
                if (showBalance) interimBalance = previousBalance + delta
                val year = cursor.getInt(columnIndexGroupYear)
                val second = cursor.getInt(columnIndexGroupSecond)
                val groupStart = if (columnIndexGroupStart > -1) cursor.getInt(columnIndexGroupStart) else 0
                val x = calculateX(year, second, groupStart).toFloat()
                if (cursor.isFirst) {
                    val start = x - 1
                    xAxis.axisMinimum = start
                    if (showBalance) lineEntries.add(Entry(start, previousBalance.toFloat()))
                }
                barEntries.add(BarEntry(x, floatArrayOf(sumExpense.toFloat(), sumIncome.toFloat())))
                if (showBalance) {
                    lineEntries.add(Entry(x, interimBalance.toFloat()))
                    previousBalance = interimBalance
                }
                if (cursor.isLast) {
                    xAxis.axisMaximum = (x + 1)
                }
            } while (cursor.moveToNext())
            val combinedData = CombinedData()
            val valueFormatter = IValueFormatter { value: Float, _: Entry?, _: Int, _: ViewPortHandler? -> convertAmount(value) }
            val set1 = BarDataSet(barEntries, "")
            set1.stackLabels = arrayOf(
                    getString(R.string.history_chart_out_label),
                    getString(R.string.history_chart_in_label))
            val barColors = listOf(ContextCompat.getColor(context, R.color.colorExpenseHistoryChart), ContextCompat.getColor(context, R.color.colorIncomeHistoryChart))
            val textColors = listOf(ContextCompat.getColor(context, R.color.colorExpense), ContextCompat.getColor(context, R.color.colorIncome))
            set1.colors = barColors
            set1.setValueTextColors(textColors)
            set1.isUseTextColorsOnYAxis = true
            set1.valueTextSize = valueTextSize
            set1.setDrawValues(showTotals)
            set1.valueFormatter = valueFormatter
            val barWidth = 0.45f
            val barData = BarData(set1)
            barData.barWidth = barWidth
            combinedData.setData(barData)
            if (showBalance) {
                val set2 = LineDataSet(lineEntries, getString(R.string.current_balance))
                set2.valueTextSize = valueTextSize
                set2.lineWidth = 2.5f
                val balanceColor = ContextCompat.getColor(context, R.color.emphasis)
                set2.color = balanceColor
                set2.valueTextColor = textColor
                set2.valueFormatter = valueFormatter
                set2.setDrawValues(showTotals)
                val lineData = LineData(set2)
                combinedData.setData(lineData)
            }
            with(binding.historyChart) {
                data = combinedData
                setHighlighter(ExactStackedBarHighlighter.CombinedHighlighter(this))
                invalidate()
            }
        } else {
            binding.historyChart.clear()
        }
    }

    private fun configureYAxis(yAxis: YAxis) {
        yAxis.textColor = textColor
        yAxis.valueFormatter = IAxisValueFormatter { value: Float, _: AxisBase? -> convertAmount(value) }
    }

    private fun convertAmount(value: Float): String {
        return currencyFormatter.convAmount(value.toLong(), accountInfo.currency)
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        binding.historyChart.clear()
    }

    companion object {
        private const val GROUPING_CURSOR = 1
        private const val MONTH_GROUPING_YEAR_X = 12

        //julian day 0 is monday -> Only if week starts with monday it divides without remainder by 7
        //for the x axis we need an Integer for proper rendering, for printing the week range, we add the offset from monday
        private val JULIAN_DAY_WEEK_OFFSET = if (DatabaseConstants.weekStartsOn == Calendar.SUNDAY) 6 else DatabaseConstants.weekStartsOn - Calendar.MONDAY
    }
}