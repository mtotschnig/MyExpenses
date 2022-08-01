package org.totschnig.myexpenses.export

import kotlin.Throws
import org.totschnig.myexpenses.export.qif.QifBufferedReader
import org.totschnig.myexpenses.export.qif.QifUtils
import java.io.IOException

data class CategoryInfo(val name: String, val income: Boolean = false) {
    companion object {
        //originally based on Financisto
        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(r: QifBufferedReader): CategoryInfo? {
            var name: String? = null
            var isIncome = false
            while (true) {
                val line = r.readLine() ?: break
                if (line.startsWith("^")) {
                    break
                }
                if (line.startsWith("N")) {
                    name = QifUtils.trimFirstChar(line)
                } else if (line.startsWith("I")) {
                    isIncome = true
                }
            }
            return name?.let { CategoryInfo(it, isIncome) }
        }
    }
}