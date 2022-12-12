package org.totschnig.myexpenses.dialog

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Spinner
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.enumValueOrDefault

fun Spinner.configureDateFormat(
    context: Context,
    prefHandler: PrefHandler,
    prefName: String
) {
    adapter = ArrayAdapter(
        context, android.R.layout.simple_spinner_item, QifDateFormat.values()
    ).apply {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }
    setSelection(
        enumValueOrDefault(
            prefHandler.getString(prefName),
            QifDateFormat.default
        ).ordinal
    )
}