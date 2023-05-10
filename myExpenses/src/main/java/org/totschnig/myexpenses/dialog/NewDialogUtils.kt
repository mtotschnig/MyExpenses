package org.totschnig.myexpenses.dialog

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrDefault

fun Spinner.configureDateFormat(
    context: Context,
    prefHandler: PrefHandler,
    prefName: String
) {
    adapter = ArrayAdapter(
        context, android.R.layout.simple_spinner_item, QifDateFormat.values()
    ).apply {
        setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }
    setSelection(prefHandler.enumValueOrDefault(prefName, QifDateFormat.default).ordinal)
}

fun configureCalendarRestoreStrategy(view: View, prefHandler: PrefHandler): RadioGroup {
    val restorePlanStrategy = view.findViewById<RadioGroup>(R.id.restore_calendar_handling)
    val calendarId = prefHandler.requireString(PrefKey.PLANNER_CALENDAR_ID,"-1")
    val calendarPath = prefHandler.requireString(PrefKey.PLANNER_CALENDAR_PATH,"")
    val configured = view.findViewById<RadioButton>(R.id.restore_calendar_handling_configured)
    if (calendarId == "-1" || calendarPath == "") {
        configured.visibility = View.GONE
    } else {
        view.findViewById<View>(R.id.restore_calendar_handling_create_new).visibility = View.GONE
        //noinspection SetTextI18n
        configured.text =
            "${view.context.getString(R.string.restore_calendar_handling_configured)} ($calendarPath)"
    }
    return restorePlanStrategy
}