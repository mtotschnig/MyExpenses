package org.totschnig.myexpenses.preference

import android.content.DialogInterface
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.os.Bundle
import android.provider.CalendarContract
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.PreferenceDialogFragmentCompat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.INVALID_CALENDAR_ID
import org.totschnig.myexpenses.provider.PLANNER_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.PLANNER_CALENDAR_NAME
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.requireString
import javax.inject.Inject

class CalendarListPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    @Inject
    lateinit var plannerUtils: PlannerUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val preference = preference as ListPreference
        var localExists = false
        val selectionCursor: Cursor
        val value = preference.value
        var selectedIndex = -1
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.NAME,
            "ifnull(" + CalendarContract.Calendars.ACCOUNT_NAME + ",'') || ' / ' ||" +
                    "ifnull(" + CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + ",'') AS full_name"
        )
        val calCursor = try {
            requireContext().contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                null,
                CalendarContract.Calendars._ID + " ASC"
            )
        } catch (e: SecurityException) {
            null
            // android.permission.READ_CALENDAR or android.permission.WRITE_CALENDAR missing
        }
        if (calCursor != null) {
            if (calCursor.moveToFirst()) {
                do {
                    if (calCursor.getString(0) == value) {
                        selectedIndex = calCursor.position
                    }
                    if (calCursor.requireString(1) == PLANNER_ACCOUNT_NAME && calCursor.requireString(
                            2
                        ) == CalendarContract.ACCOUNT_TYPE_LOCAL && calCursor.requireString(3) == PLANNER_CALENDAR_NAME
                    ) localExists = true
                } while (calCursor.moveToNext())
            }
            selectionCursor = if (localExists) {
                calCursor
            } else {
                val extras = MatrixCursor(
                    arrayOf(
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.Calendars.NAME,
                        "full_name"
                    )
                )
                extras.addRow(
                    arrayOf(
                        "-1", "", "", "",
                        requireContext().getString(R.string.pref_planning_calendar_create_local)
                    )
                )
                MergeCursor(arrayOf(calCursor, extras))
            }
            selectionCursor.moveToFirst()
            builder.setSingleChoiceItems(
                selectionCursor, selectedIndex, "full_name"
            ) { dialog: DialogInterface, which: Int ->
                val itemId = (dialog as AlertDialog).listView.getItemIdAtPosition(which)
                if (itemId == -1L) {
                    //TODO: use Async Task Strict Mode violation
                    //noinspection MissingPermission
                    val plannerId: String = plannerUtils.createPlanner(false)
                    val success = plannerId != INVALID_CALENDAR_ID
                    (activity as ProtectedFragmentActivity?)!!.showSnackBar(
                        if (success) R.string.planner_create_calendar_success else R.string.planner_create_calendar_failure
                    )
                    if (success) {
                        preference.value = plannerId
                    }
                } else {
                    if (preference.callChangeListener(itemId)) {
                        preference.value = itemId.toString()
                    }
                }
                onClick(
                    dialog,
                    DialogInterface.BUTTON_POSITIVE
                )
                dialog.dismiss()
            }
        } else {
            builder.setMessage("Calendar provider not available")
        }
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(b: Boolean) {
        //nothing to do since directly handled in onClickListener of SingleChoiceItems
    }

    companion object {
        fun newInstance(key: String?): CalendarListPreferenceDialogFragmentCompat {
            val fragment = CalendarListPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}
