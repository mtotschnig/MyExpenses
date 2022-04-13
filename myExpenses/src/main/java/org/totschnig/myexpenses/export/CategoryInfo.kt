package org.totschnig.myexpenses.export

import kotlin.Throws
import org.totschnig.myexpenses.export.qif.QifBufferedReader
import org.totschnig.myexpenses.export.qif.QifUtils
import java.io.IOException

data class CategoryInfo(val name: String?, val income: Boolean) {
    companion object {
        //originally based on Financisto
        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(r: QifBufferedReader): CategoryInfo {
            var line: String
            var name: String? = null
            var isIncome = false
            while (r.readLine().also { line = it } != null) {
                if (line.startsWith("^")) {
                    break
                }
                if (line.startsWith("N")) {
                    name = QifUtils.trimFirstChar(line)
                } else if (line.startsWith("I")) {
                    isIncome = true
                }
            }
            return CategoryInfo(name, isIncome)
        }
    }
}