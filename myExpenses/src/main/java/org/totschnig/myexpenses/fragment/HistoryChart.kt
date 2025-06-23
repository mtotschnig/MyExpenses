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
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.filter.FilterCard
import org.totschnig.myexpenses.databinding.HistoryChartBinding
import org.totschnig.myexpenses.dialog.TransactionListComposeDialogFragment
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.KEY_FILTER
import org.totschnig.myexpenses.ui.ExactStackedBarHighlighter
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.getLocale
import org.totschnig.myexpenses.util.setEnabledAndVisible
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.viewmodel.HistoryViewModel
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import org.totschnig.myexpenses.viewmodel.data.HistoryAccountInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.JulianFields
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class HistoryChart : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var _binding: HistoryChartBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var accountInfo: HistoryAccountInfo

    val grouping: Grouping
        get() = viewModel.grouping.value
    private var filter: Criterion? = null
    private var valueTextSize = 10f

    @ColorInt
    private var textColor = Color.WHITE

    @Inject
    lateinit var currencyFormatter: ICurrencyFormatter

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    private var showBalance = true

    private var includeTransfers = false

    private var showTotals = true

    private val viewModel: HistoryViewModel by activityViewModels()

    val accountId
        get() = requireActivity().intent.getLongExtra(
            DatabaseConstants.KEY_ACCOUNTID,
            0
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@HistoryChart)
            inject(viewModel)
        }
        setHasOptionsMenu(true)
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(
            android.R.attr.textAppearanceSmall,
            typedValue,
            true
        )
        val textSizeAttr = intArrayOf(android.R.attr.textSize)
        val indexOfAttrTextSize = 0
        requireActivity().withStyledAttributes(typedValue.data, textSizeAttr) {
            valueTextSize =
                getDimensionPixelSize(indexOfAttrTextSize, 10) / resources.displayMetrics.density
        }
        textColor =
            UiUtils.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.grouping.collect {
                    requireActivity().invalidateOptionsMenu()
                    if (::accountInfo.isInitialized) {
                        reset()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountInfo(
                    accountId
                ).collect { (account, grouping) ->
                    val currency = currencyContext[account.currency]
                    accountInfo = HistoryAccountInfo(
                        account.id,
                        account.getLabelForScreenTitle(requireActivity()),
                        currency,
                        account.color,
                        Money(currency, account.openingBalance),
                        grouping
                    )
                    (requireActivity() as ProtectedFragmentActivity).supportActionBar?.title =
                        accountInfo.label
                    requireActivity().invalidateOptionsMenu()
                    reset()
                }
            }
        }
        IntentCompat.getParcelableExtra(requireActivity().intent, KEY_FILTER, Criterion::class.java)
            ?.let {
                filter = it
            }
        showBalance = accountId != HOME_AGGREGATE_ID && prefHandler.getBoolean(
            PrefKey.HISTORY_SHOW_BALANCE,
            showBalance
        )
        includeTransfers =
            prefHandler.getBoolean(PrefKey.HISTORY_INCLUDE_TRANSFERS, includeTransfers)
        showTotals = prefHandler.getBoolean(PrefKey.HISTORY_SHOW_TOTALS, showTotals)
        _binding = HistoryChartBinding.inflate(inflater, container, false)
        with(binding.historyChart) {
            description.isEnabled = false
            with(xAxis) {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IAxisValueFormatter { value: Float, axis: AxisBase ->
                    if (axis.axisMinimum == value) "" else formatXValue(value)
                }
                textColor = this@HistoryChart.textColor
            }
            configureYAxis(axisLeft)
            configureYAxis(axisRight)
            legend.textColor = textColor
            isHighlightPerDragEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, h: Highlight) {
                    if (h.stackIndex > -1) {
                        TransactionListComposeDialogFragment.newInstance(
                            TransactionListViewModel.LoadingInfo(
                                accountId = accountInfo.accountId,
                                currency = accountInfo.currencyUnit,
                                grouping = grouping,
                                groupingClause = listOfNotNull(
                                    buildGroupingClause(e.x.toInt()),
                                    filter?.getSelectionForParts()
                                ).joinToString(" AND "),
                                groupingArgs = filter?.getSelectionArgs(true) ?: emptyArray(),
                                label = formatXValue(e.x),
                                type = h.stackIndex == 1,//expense is first entry, income second
                                withTransfers = includeTransfers
                            )
                        ).show(parentFragmentManager, "List")
                    }
                }

                override fun onNothingSelected() {}
            })
        }
        filter?.let {
            binding.filterCard.setContent {
                FilterCard(it)
            }
        }
        return binding.root
    }

    private fun formatXValue(value: Float): String = when (grouping) {
        Grouping.DAY -> LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, value.toLong())
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))

        Grouping.WEEK -> LocalDateTime.MIN.with(
            JulianFields.JULIAN_DAY,
            julianDayFromWeekNumber(value)
        )
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))

        Grouping.MONTH -> Grouping.getDisplayTitleForMonth(
            (value / MONTH_GROUPING_YEAR_X).toInt(),
            (value % MONTH_GROUPING_YEAR_X).toInt(),
            FormatStyle.SHORT,
            requireActivity().getLocale(),
            prefHandler.monthStart
        )

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

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.history, menu)
        inflater.inflate(R.menu.grouping, menu.findItem(R.id.GROUPING_COMMAND).subMenu)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.GROUPING_COMMAND).subMenu?.let {
            it.findItem(R.id.GROUPING_NONE_COMMAND).isVisible = false
            Utils.configureGroupingMenu(it, grouping)
        }
        menu.findItem(R.id.TOGGLE_BALANCE_COMMAND)?.let {
            it.setEnabledAndVisible(accountId != HOME_AGGREGATE_ID)
            it.isChecked = showBalance
        }
        menu.findItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND)?.isChecked = includeTransfers
        menu.findItem(R.id.TOGGLE_TOTALS_COMMAND)?.isChecked = showTotals
    }

    @Deprecated("Deprecated in Java")
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
                viewModel.persistGrouping(newGrouping)
            }
            return true
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (id == GROUPING_CURSOR) {
            val (builder, selection, selectionArgs) = accountInfo.groupingQuery(filter)
            if (shouldUseGroupStart()) {
                builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_JULIAN_START)
            }
            if (includeTransfers) {
                builder.appendBooleanQueryParameter(TransactionProvider.QUERY_PARAMETER_INCLUDE_TRANSFERS)
            }
            return CursorLoader(
                requireActivity(),
                builder.build(),
                null, selection, selectionArgs, null
            )
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
            val columnIndexGroupSumExpense =
                cursor.getColumnIndex(DatabaseConstants.KEY_SUM_EXPENSES)
            val columnIndexGroupSumTransfer =
                cursor.getColumnIndex(DatabaseConstants.KEY_SUM_TRANSFERS)
            val columnIndexGroupYear = cursor.getColumnIndex(DatabaseConstants.KEY_YEAR)
            val columnIndexGroupSecond = cursor.getColumnIndex(DatabaseConstants.KEY_SECOND_GROUP)
            val columnIndexGroupStart = cursor.getColumnIndex(DatabaseConstants.KEY_GROUP_START)
            val barEntries = ArrayList<BarEntry>()
            val lineEntries = ArrayList<Entry>()
            val xAxis = binding.historyChart.xAxis
            var runningBalance = accountInfo.openingBalance.amountMinor
            do {
                val sumIncome = cursor.getLong(columnIndexGroupSumIncome)
                val sumExpense = cursor.getLong(columnIndexGroupSumExpense)
                val sumTransfer =
                    if (columnIndexGroupSumTransfer > -1) cursor.getLong(columnIndexGroupSumTransfer) else 0
                val delta = sumIncome + sumExpense + sumTransfer
                val year = cursor.getInt(columnIndexGroupYear)
                val second = cursor.getInt(columnIndexGroupSecond)
                val groupStart =
                    if (columnIndexGroupStart > -1) cursor.getInt(columnIndexGroupStart) else 0
                val x = calculateX(year, second, groupStart).toFloat()
                if (cursor.isFirst) {
                    val start = x - 1
                    xAxis.axisMinimum = start
                    if (showBalance) lineEntries.add(Entry(start, runningBalance.toFloat()))
                }
                barEntries.add(BarEntry(x, floatArrayOf(sumExpense.toFloat(), sumIncome.toFloat())))
                if (showBalance) {
                    runningBalance += delta
                    lineEntries.add(Entry(x, runningBalance.toFloat()))
                }
                if (cursor.isLast) {
                    xAxis.axisMaximum = (x + 1)
                }
            } while (cursor.moveToNext())
            val combinedData = CombinedData()
            val valueFormatter =
                IValueFormatter { value: Float, _: Entry?, _: Int, _: ViewPortHandler? ->
                    convertAmount(value)
                }
            val set1 = BarDataSet(barEntries, "")
            set1.stackLabels = arrayOf(
                getString(R.string.history_chart_out_label),
                getString(R.string.history_chart_in_label)
            )
            val barColors = listOf(
                ContextCompat.getColor(context, R.color.colorExpenseHistoryChart),
                ContextCompat.getColor(context, R.color.colorIncomeHistoryChart)
            )
            val textColors = listOf(
                ContextCompat.getColor(context, R.color.colorExpense),
                ContextCompat.getColor(context, R.color.colorIncome)
            )
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
        yAxis.valueFormatter =
            IAxisValueFormatter { value: Float, _: AxisBase? -> convertAmount(value) }
    }

    private fun convertAmount(value: Float) =
        currencyFormatter.convAmount(value.toLong(), accountInfo.currencyUnit)

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        binding.historyChart.clear()
    }

    companion object {
        private const val GROUPING_CURSOR = 1
        private const val MONTH_GROUPING_YEAR_X = 12

        //julian day 0 is monday -> Only if week starts with monday it divides without remainder by 7
        //for the x axis we need an Integer for proper rendering, for printing the week range, we add the offset from monday
        private val JULIAN_DAY_WEEK_OFFSET =
            if (DatabaseConstants.weekStartsOn == Calendar.SUNDAY) 6 else DatabaseConstants.weekStartsOn - Calendar.MONDAY
    }
}