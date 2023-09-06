package org.totschnig.myexpenses.export

import kotlin.Throws
import org.totschnig.myexpenses.export.qif.QifBufferedReader
import org.totschnig.myexpenses.export.qif.QifUtils
import java.io.IOException

data class CategoryInfo(val name: String, val income: Boolean = false)