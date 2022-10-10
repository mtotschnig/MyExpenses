package org.totschnig.myexpenses.provider.filter

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS

@Parcelize
class TagCriterion(
    override val label: String?,
    override val values: Array<Long>
) : IdCriterion() {
    constructor(label: String, vararg values: Long) : this(label, values.toTypedArray())

    @IgnoredOnParcel
    override val operation = WhereFilter.Operation.IN

    override val selection: String
        get() = "$KEY_ROWID IN (SELECT $KEY_TRANSACTIONID FROM $TABLE_TRANSACTIONS_TAGS WHERE ${super.selection})"

    @IgnoredOnParcel
    override val id = R.id.FILTER_TAG_COMMAND

    @IgnoredOnParcel
    override val column = KEY_TAGID

    companion object {
        fun fromStringExtra(extra: String) = parseStringExtra(extra)?.let {
            TagCriterion(it.first, *it.second)
        }
    }
}