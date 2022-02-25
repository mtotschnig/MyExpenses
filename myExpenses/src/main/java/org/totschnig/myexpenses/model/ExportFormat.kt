package org.totschnig.myexpenses.model

import org.totschnig.myexpenses.R
import java.util.*

enum class ExportFormat(val mimeType: String, val resId: Int) {
    QIF("application/qif", R.id.qif),
    CSV("text/csv", R.id.csv),
    JSON("application/json", R.id.json);

    val extension: String
        get() = name.lowercase(Locale.US)
}