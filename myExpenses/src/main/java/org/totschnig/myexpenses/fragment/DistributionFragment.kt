package org.totschnig.myexpenses.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.CategoryTreeAdapter
import org.totschnig.myexpenses.databinding.CategoryRowBinding
import org.totschnig.myexpenses.databinding.DistributionListBinding
import org.totschnig.myexpenses.databinding.DistributionListInnerBinding
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.DistributionViewModel
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber
import kotlin.math.abs

class DistributionFragment : DistributionBaseFragment<CategoryRowBinding?>() {
    var showChart = false
    private var textColorSecondary = 0
    private var _binding: DistributionListBinding? = null
    private val binding
        get() = _binding!!
    private var _innerBinding: DistributionListInnerBinding? = null
    private val innerBinding
        get() = _innerBinding!!
    val viewModel: DistributionViewModel by viewModels()

    public override fun getListView(): ExpandableListView {
        return innerBinding.list
    }

    private val chart: PieChart
        get() = innerBinding.chart1

    override fun onCreate(savedInstanceState: Bundle?) {
        (requireActivity().application as MyApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel.account(requireActivity().intent.getLongExtra(DatabaseConstants.KEY_ACCOUNTID, 0)).observe(viewLifecycleOwner) {
            accountInfo = DistributionAccountInfo(it.id, it.getLabelForScreenTitle(requireActivity()), it.currencyUnit, it.color)
            updateSum()
            updateDateInfo()
            (requireActivity() as ProtectedFragmentActivity).supportActionBar?.title = accountInfo.label
            updateColor()
            initListView()
        }

        val ctx = requireActivity() as ProtectedFragmentActivity
        showChart = prefHandler.getBoolean(PrefKey.DISTRIBUTION_SHOW_CHART, true)
        val b = savedInstanceState ?: ctx.intent?.extras
        grouping = b?.getString(DatabaseConstants.KEY_GROUPING)?.let { Grouping.valueOf(it) } ?: Grouping.NONE
        groupingYear = b?.getInt(DatabaseConstants.KEY_YEAR) ?: 0
        groupingSecond = b?.getInt(DatabaseConstants.KEY_SECOND_GROUP) ?: 0
        ctx.invalidateOptionsMenu()
        _binding = DistributionListBinding.inflate(inflater, container, false)
        _innerBinding = DistributionListInnerBinding.bind(binding.root)
        textColorSecondary = ctx.textColorSecondary.defaultColor
        with(chart) {
            visibility = if (showChart) View.VISIBLE else View.GONE
            description.isEnabled = false
            setExtraOffsets(20f, 0f, 20f, 0f)
            renderer = SelectivePieChartRenderer(chart, object : SelectivePieChartRenderer.Selector {
                var lastValueGreaterThanOne = true
                override fun shouldDrawEntry(index: Int, pieEntry: PieEntry, value: Float): Boolean {
                    val greaterThanOne = value > 1f
                    val shouldDraw = greaterThanOne || lastValueGreaterThanOne
                    lastValueGreaterThanOne = greaterThanOne
                    return shouldDraw
                }
            }).apply {
                paintEntryLabels.color = textColorSecondary
                paintEntryLabels.textSize = getTextSizeForAppearance(android.R.attr.textAppearanceSmall).toFloat()
            }
            setCenterTextSizePixels(getTextSizeForAppearance(android.R.attr.textAppearanceMedium).toFloat())
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, highlight: Highlight) {
                    val index = highlight.x.toInt()
                    val packedPosition = if (lastExpandedPosition == -1) ExpandableListView.getPackedPositionForGroup(index) else ExpandableListView.getPackedPositionForChild(lastExpandedPosition, index)
                    Timber.w("%d-%d-%d, %b", index, lastExpandedPosition, packedPosition, showChart)
                    val listView = listView
                    val flatPosition = listView.getFlatListPosition(packedPosition)
                    listView.setItemChecked(flatPosition, true)
                    listView.smoothScrollToPosition(flatPosition)
                    setCenterText(index)
                }

                override fun onNothingSelected() {
                    this@DistributionFragment.onNothingSelected()
                }
            })
            setUsePercentValues(true)
        }
        return binding.root
    }

    private fun initListView() {
        with(listView) {
            emptyView = binding.empty
            mAdapter = CategoryTreeAdapter(requireContext(), currencyFormatter, accountInfo.currency, showChart, showChart, false)
            setAdapter(mAdapter)
            setOnGroupClickListener { _: ExpandableListView?, _: View?, groupPosition: Int, _: Long ->
                if (showChart) {
                    if (mAdapter.getChildrenCount(groupPosition) == 0) {
                        val packedPosition = ExpandableListView.getPackedPositionForGroup(groupPosition)
                        listView.setItemChecked(listView.getFlatListPosition(packedPosition), true)
                        if (lastExpandedPosition != -1
                                && groupPosition != lastExpandedPosition) {
                            listView.collapseGroup(lastExpandedPosition)
                            lastExpandedPosition = -1
                        }
                        if (lastExpandedPosition == -1) {
                            highlight(groupPosition)
                        }
                        return@setOnGroupClickListener true
                    }
                }
                false
            }
            setOnGroupExpandListener { groupPosition: Int ->
                if (showChart) {
                    if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
                        listView.collapseGroup(lastExpandedPosition)
                    }
                    lastExpandedPosition = groupPosition
                    setData()
                    highlight(0)
                } else {
                    lastExpandedPosition = groupPosition
                }
            }
            setOnChildClickListener { _: ExpandableListView?, _: View?, groupPosition: Int, childPosition: Int, _: Long ->
                if (showChart) {
                    val packedPosition = ExpandableListView.getPackedPositionForChild(
                            groupPosition, childPosition)
                    highlight(childPosition)
                    val flatPosition = listView.getFlatListPosition(packedPosition)
                    listView.setItemChecked(flatPosition, true)
                    return@setOnChildClickListener true
                }
                false
            }
            setOnGroupCollapseListener { groupPosition: Int ->
                lastExpandedPosition = -1
                if (showChart) {
                    setData()
                    highlight(groupPosition)
                }
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View,
                                            position: Int, id: Long) {
                    if (showChart) {
                        val pos = listView.getExpandableListPosition(position)
                        val type = ExpandableListView.getPackedPositionType(pos)
                        val group = ExpandableListView.getPackedPositionGroup(pos)
                        val child = ExpandableListView.getPackedPositionChild(pos)
                        val highlightedPos = if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                            if (lastExpandedPosition != group) {
                                listView.collapseGroup(lastExpandedPosition)
                            }
                            if (lastExpandedPosition == -1) group else -1
                        } else {
                            child
                        }
                        highlight(highlightedPos)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            choiceMode = AbsListView.CHOICE_MODE_SINGLE
            registerForContextMenu(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _innerBinding = null
    }

    fun onNothingSelected() {
        val listView = listView
        listView.setItemChecked(listView.checkedItemPosition, false)
    }

    private fun getTextSizeForAppearance(appearance: Int): Int {
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(appearance, typedValue, true)
        val textSizeAttr = intArrayOf(android.R.attr.textSize)
        val indexOfAttrTextSize = 0
        val a = requireActivity().obtainStyledAttributes(typedValue.data, textSizeAttr)
        val textSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1)
        a.recycle()
        return textSize
    }

    override fun getSecondarySort(): Any {
        return "abs(" + DatabaseConstants.KEY_SUM + ") DESC"
    }

    override fun onLoadFinished() {
        super.onLoadFinished()
        if (mAdapter.groupCount > 0) {
            chart.visibility = if (showChart) View.VISIBLE else View.GONE
            setData()
            highlight(0)
            if (showChart) {
                listView.setItemChecked(listView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(0)), true)
            }
        } else {
            chart.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.distribution, menu)
        inflater.inflate(R.menu.grouping, menu)
        val typeButton: SwitchCompat = menu.findItem(R.id.switchId).actionView.findViewById(R.id.TaType)
        typeButton.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> setType(isChecked) }
    }

    fun setType(isChecked: Boolean) {
        isIncome = isChecked
        reset()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        Utils.configureGroupingMenu(menu.findItem(R.id.GROUPING_COMMAND).subMenu, grouping)
        val m = menu.findItem(R.id.TOGGLE_CHART_COMMAND)
        if (m != null) {
            m.isChecked = showChart
        }
        val item = menu.findItem(R.id.switchId)
        Utils.menuItemSetEnabledAndVisible(item, !aggregateTypes)
        if (!aggregateTypes) {
            (item.actionView.findViewById<View>(R.id.TaType) as SwitchCompat).isChecked = isIncome
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun updateIncomeAndExpense(income: Long, expense: Long) {
        updateSum("+", binding.sumIncome, income)
        updateSum("-", binding.sumExpense, expense)
    }

    @SuppressLint("SetTextI18n")
    private fun updateSum(prefix: String, tv: TextView, amount: Long) {
        tv.text = prefix + currencyFormatter.formatCurrency(
                Money(accountInfo.currency, amount))
    }

    private fun handleGrouping(item: MenuItem): Boolean {
        val newGrouping = Utils.getGroupingFromMenuItemId(item.itemId)
        if (newGrouping != null) {
            if (!item.isChecked) {
                grouping = newGrouping
                setDefaults()
                requireActivity().invalidateOptionsMenu()
                reset()
            }
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (handleGrouping(item)) return true
        if (super.onOptionsItemSelected(item)) return true
        if (item.itemId == R.id.TOGGLE_CHART_COMMAND) {
            showChart = !showChart
            prefHandler.putBoolean(PrefKey.DISTRIBUTION_SHOW_CHART, showChart)
            chart.visibility = if (showChart) View.VISIBLE else View.GONE
            if (showChart) {
                collapseAll()
            } else {
                onNothingSelected()
            }
            mAdapter.toggleColors()
            return true
        }
        return false
    }

    override fun showAllCategories(): Boolean {
        return false
    }

    private fun setData() {
        val categories: List<Category>
        val parent: Category?
        if (lastExpandedPosition == -1) {
            parent = null
            categories = mAdapter.mainCategories
        } else {
            parent = mAdapter.getGroup(lastExpandedPosition)
            categories = mAdapter.getSubCategories(lastExpandedPosition)
        }
        with(chart) {
            data = PieData(PieDataSet(categories.map { PieEntry(abs(it.sum?.toFloat() ?: 0F), it.label) }, "").apply {
                colors = parent?.let { mAdapter.getSubColors(it.color) }
                        ?: categories.map(Category::color)
                sliceSpace = 2f
                setDrawValues(false)
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart2Length = 0.1f
                valueLineColor = textColorSecondary
            }).apply {
                setValueFormatter(PercentFormatter())
            }
            legend.isEnabled = false
            highlightValues(null)
            invalidate()
        }
    }

    private fun highlight(position: Int) {
        if (position != -1) {
            chart.highlightValue(position.toFloat(), 0)
            setCenterText(position)
        }
    }

    private fun setCenterText(position: Int) {
        val data = chart.data
        val entry = data.dataSet.getEntryForIndex(position)
        val description = entry.label
        val value = data.dataSet.valueFormatter.getFormattedValue(
                entry.value / data.yValueSum * 100f,
                entry, position, null)
        chart.centerText = """
            $description
            $value
            """.trimIndent()
    }

    private fun updateColor() {
        binding.BottomLine.setBackgroundColor(accountInfo.color)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(DatabaseConstants.KEY_GROUPING, grouping)
        outState.putInt(DatabaseConstants.KEY_YEAR, groupingYear)
        outState.putInt(DatabaseConstants.KEY_SECOND_GROUP, groupingSecond)
    }

    override val prefKey = PrefKey.DISTRIBUTION_AGGREGATE_TYPES
}