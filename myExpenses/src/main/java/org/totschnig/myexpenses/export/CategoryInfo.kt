package org.totschnig.myexpenses.export

import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.asCategoryType

data class CategoryInfo(val name: String, val type: Byte = FLAG_NEUTRAL) {
    constructor(name: String, income: Boolean) : this(name, income.asCategoryType)
}