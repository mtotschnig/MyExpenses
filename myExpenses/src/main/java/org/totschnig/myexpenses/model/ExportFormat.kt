package org.totschnig.myexpenses.model

import java.util.*

enum class ExportFormat(val mimeType: String) {
    QIF("application/qif"),
    CSV("text/csv"),
    JSON("application/json");

    val extension: String
        get() = name.lowercase(Locale.US)
}