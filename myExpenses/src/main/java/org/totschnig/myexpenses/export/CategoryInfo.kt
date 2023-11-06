package org.totschnig.myexpenses.export

import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL

data class CategoryInfo(val name: String, val type: UByte = FLAG_NEUTRAL) {
    constructor(name: String, income: Boolean) : this(name, if (income) FLAG_INCOME else FLAG_EXPENSE)
}