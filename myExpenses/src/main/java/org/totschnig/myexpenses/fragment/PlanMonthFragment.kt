package org.totschnig.myexpenses.fragment

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidGridAdapter
import com.roomorama.caldroid.CaldroidListener
import com.roomorama.caldroid.CalendarHelper
import com.roomorama.caldroid.CellView
import hirondelle.date4j.DateTime
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.HELP_VARIANT_PLANS
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.dialog.OrphanedTransactionDialog
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.CalendarProviderProxy
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ColorUtils.isBrightColor
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ui.UiUtils
import org.totschnig.myexpenses.viewmodel.PlanInstanceInfo
import org.totschnig.myexpenses.viewmodel.data.PlanInstanceState
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class PlanMonthFragment : CaldroidFragment(), LoaderManager.LoaderCallbacks<Cursor?> {
    private var mManager: LoaderManager? = null
    private var readOnly = false
    var stateListDrawable: StateListDrawable? = null

    @State
    var instance2TransactionMap = HashMap<Long, Long>()

    //caldroid fragment operates on Dates set to Midnight. We want to store the exact timestamp in order
    //create the transactions with the exact date provided by the calendar
    @State
    var dateTime2TimeStampMap = HashMap<DateTime, Long>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readOnly = requireArguments().getBoolean(KEY_READ_ONLY)
        StateSaver.restoreInstanceState(this, savedInstanceState)
        caldroidListener = object : CaldroidListener() {
            override fun onChangeMonth(month: Int, year: Int) {
                requireLoader(INSTANCES_CURSOR)
            }
        }
        setupStateListDrawable()
    }

    private fun setupStateListDrawable() {
        val accountColor = requireArguments().getInt(DatabaseConstants.KEY_COLOR)
        stateListDrawable = StateListDrawable()
        val surfaceColor =
            UiUtils.getColor(requireContext(), com.google.android.material.R.attr.colorSurface)
        val todayDrawableResId = R.drawable.red_border
        val today = AppCompatResources.getDrawable(requireContext(), todayDrawableResId)!!
            .mutate() as GradientDrawable
        val todaySelected = AppCompatResources.getDrawable(requireContext(), todayDrawableResId)!!
            .mutate() as GradientDrawable
        todaySelected.setColor(accountColor)
        today.setColor(surfaceColor)
        stateListDrawable!!.addState(
            intArrayOf(android.R.attr.state_activated),
            ColorDrawable(ResourcesCompat.getColor(resources, R.color.appDefault, null))
        )
        stateListDrawable!!.addState(
            intArrayOf(
                com.caldroid.R.attr.state_date_selected,
                com.caldroid.R.attr.state_date_today
            ),
            todaySelected
        )
        stateListDrawable!!.addState(
            intArrayOf(com.caldroid.R.attr.state_date_selected),
            ColorDrawable(accountColor)
        )
        stateListDrawable!!.addState(
            intArrayOf(com.caldroid.R.attr.state_date_today),
            today
        )
        stateListDrawable!!.addState(
            intArrayOf(com.caldroid.R.attr.state_date_prev_next_month),
            ColorDrawable(
                ResourcesCompat.getColor(
                    resources,
                    R.color.caldroid_state_date_prev_next_month,
                    null
                )
            )
        )
        stateListDrawable!!.addState(
            intArrayOf(),
            ColorDrawable(surfaceColor)
        )
    }

    private fun requireLoader(loaderId: Int) {
        Utils.requireLoader(mManager, loaderId, null, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mManager = LoaderManager.getInstance(this)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val toolbar = view!!.findViewById<Toolbar>(com.caldroid.R.id.calendar_toolbar)
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            (requireActivity() as ProtectedFragmentActivity).dispatchCommand(
                item.itemId,
                HELP_VARIANT_PLANS
            )
            true
        }
        toolbar.inflateMenu(R.menu.help_with_icon)
        toolbar.title = requireArguments().getString(TOOLBAR_TITLE)
        requireLoader(INSTANCE_STATUS_CURSOR)
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun getNewDatesGridAdapter(month: Int, year: Int): CaldroidGridAdapter {
        return CaldroidCustomAdapter(activity, month, year, getCaldroidData(), extraData)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        when (id) {
            INSTANCES_CURSOR -> {
                // Construct the query with the desired date range.
                val builder = CalendarProviderProxy.INSTANCES_URI.buildUpon()
                val startOfMonth = DateTime(year, month, 1, 0, 0, 0, 0)
                val start = startOfMonth.minusDays(7)
                    .getMilliseconds(TimeZone.getDefault())
                val end = startOfMonth.endOfMonth.plusDays(7)
                    .getMilliseconds(TimeZone.getDefault())
                ContentUris.appendId(builder, start)
                ContentUris.appendId(builder, end)
                return CursorLoader(
                    requireActivity(),
                    builder.build(),
                    null, String.format(
                        Locale.US, CalendarContract.Instances.EVENT_ID + " = %d",
                        requireArguments().getLong(DatabaseConstants.KEY_PLANID)
                    ),
                    null,
                    null
                )
            }

            INSTANCE_STATUS_CURSOR -> return CursorLoader(
                requireActivity(),
                TransactionProvider.PLAN_INSTANCE_STATUS_URI, arrayOf(
                    DatabaseConstants.KEY_TEMPLATEID,
                    DatabaseConstants.KEY_INSTANCEID,
                    DatabaseConstants.KEY_TRANSACTIONID
                ),
                DatabaseConstants.KEY_TEMPLATEID + " = ?", arrayOf(templateId.toString()),
                null
            )
        }
        throw IllegalArgumentException()
    }

    private val templateId: Long
        get() = requireArguments().getLong(DatabaseConstants.KEY_ROWID)

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        if (data == null) {
            return
        }
        when (loader.id) {
            INSTANCES_CURSOR -> {
                val calendar = Calendar.getInstance()
                data.moveToFirst()
                clearSelectedDates()
                while (!data.isAfterLast) {
                    val timeInMillis = data.getLong(
                        data.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                    )
                    calendar.timeInMillis = timeInMillis
                    val dateTime = CalendarHelper.convertDateToDateTime(calendar.time)
                    selectedDates.add(dateTime)
                    dateTime2TimeStampMap[dateTime] = timeInMillis
                    data.moveToNext()
                }
                refreshView()
            }

            INSTANCE_STATUS_CURSOR -> {
                data.moveToFirst()
                instance2TransactionMap.clear()
                while (!data.isAfterLast) {
                    instance2TransactionMap[data.getLong(
                        data.getColumnIndexOrThrow(
                            DatabaseConstants.KEY_INSTANCEID
                        )
                    )] =
                        data.getLong(data.getColumnIndexOrThrow(DatabaseConstants.KEY_TRANSACTIONID))
                    data.moveToNext()
                }
                refreshView()
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {}

    private fun getState(id: Long): PlanInstanceState {
        val transactionId = instance2TransactionMap[id]
        return if (transactionId == null) {
            PlanInstanceState.OPEN
        } else if (transactionId != 0L) {
            PlanInstanceState.APPLIED
        } else {
            PlanInstanceState.CANCELLED
        }
    }

    private inner class CaldroidCustomAdapter(
        context: Context?, month: Int, year: Int,
        caldroidData: Map<String?, Any?>?,
        extraData: Map<String?, Any?>?
    ) : CaldroidGridAdapter(
        context, month, year, caldroidData,
        extraData
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            //noinspection InflateParams
            val frameLayout = (convertView ?: localInflater.inflate(R.layout.plan_calendar_cell, null)) as CellView
            val state = frameLayout.findViewById<ImageView>(R.id.state)
            val text = frameLayout.findViewById<TextView>(R.id.calendar_tv)
            customizeCell(position, frameLayout, text)
            val dateTime = datetimeList[position]
            val calculateId = CalendarProviderProxy.calculateId(dateTime)
            val planInstanceState = getState(calculateId)
            val brightColor = isBrightColor(requireArguments().getInt(DatabaseConstants.KEY_COLOR))
            val themeResId = if (brightColor) R.style.LightBackground else R.style.DarkBackground
            val transactionId = instance2TransactionMap[calculateId]
            if (selectedDates.contains(dateTime)) {
                state.visibility = View.VISIBLE
                when (planInstanceState) {
                    PlanInstanceState.OPEN -> {
                        state.setImageBitmap(
                            UiUtils.getTintedBitmapForTheme(
                                getContext(),
                                R.drawable.ic_stat_open,
                                themeResId
                            )
                        )
                        frameLayout.contentDescription =
                            getString(R.string.plan_instance_state_open)
                    }

                    PlanInstanceState.APPLIED -> {
                        state.setImageBitmap(
                            UiUtils.getTintedBitmapForTheme(
                                getContext(),
                                R.drawable.ic_stat_applied,
                                themeResId
                            )
                        )
                        frameLayout.contentDescription =
                            getString(R.string.plan_instance_state_applied)
                    }

                    PlanInstanceState.CANCELLED -> {
                        state.setImageBitmap(
                            UiUtils.getTintedBitmapForTheme(
                                getContext(),
                                R.drawable.ic_stat_cancelled,
                                themeResId
                            )
                        )
                        frameLayout.contentDescription =
                            getString(R.string.plan_instance_state_cancelled)
                    }
                }
                text.setTextColor(
                    ResourcesCompat.getColor(
                        getResources(),
                        if (brightColor) com.caldroid.R.color.cell_text_color else com.caldroid.R.color.cell_text_color_dark,
                        null
                    )
                )
                if (!readOnly) {
                    val templatesList = requireParentFragment() as TemplatesList
                    templatesList.configureOnClickPopup(
                        frameLayout,
                        PlanInstanceInfo(
                            templateId,
                            calculateId,
                            getDateForPosition(position),
                            transactionId,
                            planInstanceState
                        ), null, null
                    )
                }
            } else if (planInstanceState === PlanInstanceState.APPLIED) {
                state.visibility = View.VISIBLE
                state.setImageResource(R.drawable.ic_warning)
                if (!readOnly) {
                    frameLayout.setOnClickListener {
                        val relinkCandidate = selectedDates.map {
                            it to CalendarProviderProxy.calculateId(it)
                        }
                            .filter { getState(it.second) == PlanInstanceState.OPEN }
                            .minByOrNull { abs(it.first.day - dateTime.day) }?.let {
                                PlanInstanceInfo(
                                    templateId,
                                    it.second,
                                    dateTime2TimeStampMap[it.first],
                                    transactionId,
                                    PlanInstanceState.OPEN
                                )
                            }
                        OrphanedTransactionDialog.newInstance(transactionId!!, relinkCandidate)
                            .show(requireActivity().supportFragmentManager, "ORPHANED_TRANSACTIONS")
                    }
                }
            } else {
                state.visibility = View.GONE
                frameLayout.setOnClickListener(null)
            }
            return frameLayout
        }

        private fun getDateForPosition(position: Int): Long {
            val date = dateTime2TimeStampMap[datetimeList[position]]
            return date ?: System.currentTimeMillis()
        }

        override fun resetCustomResources(cellView: View, tv: TextView) {
            cellView.background = stateListDrawable!!.mutate().constantState!!.newDrawable()
            tv.setTextColor(defaultTextColorRes)
        }

        override fun isEnabled(position: Int): Boolean {
            return selectedDates.contains(datetimeList[position])
        }
    }

    companion object {
        private const val TOOLBAR_TITLE = "toolbarTitle"
        private const val KEY_READ_ONLY = "readOnly"
        const val INSTANCES_CURSOR = 1
        const val INSTANCE_STATUS_CURSOR = 2
        fun newInstance(
            title: String?, templateId: Long, planId: Long, color: Int,
            readOnly: Boolean, prefHandler: PrefHandler
        ): PlanMonthFragment {
            val f = PlanMonthFragment()
            val args = Bundle()
            args.putString(TOOLBAR_TITLE, title)
            args.putInt(THEME_RESOURCE, R.style.CaldroidCustom)
            args.putLong(DatabaseConstants.KEY_PLANID, planId)
            args.putInt(DatabaseConstants.KEY_COLOR, color)
            args.putLong(DatabaseConstants.KEY_ROWID, templateId)
            args.putBoolean(SIX_WEEKS_IN_CALENDAR, false)
            args.putBoolean(KEY_READ_ONLY, readOnly)
            args.putInt(
                START_DAY_OF_WEEK,
                prefHandler.weekStartWithFallback()
            )
            f.arguments = args
            return f
        }
    }
}