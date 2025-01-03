package org.totschnig.myexpenses.provider.filter

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS

@Parcelize
@Serializable
@SerialName(KEY_TAGID)
class TagCriterion(
    override val label: String,
    override val values: List<Long>
) : IdCriterion() {
    constructor(label: String, vararg values: Long) : this(label, values.toList())

    override fun getSelection(forExport: Boolean): String =
        "$KEY_ROWID IN (SELECT $KEY_TRANSACTIONID FROM $TABLE_TRANSACTIONS_TAGS WHERE ${super.getSelection(
            false
        )
        })"

    @IgnoredOnParcel
    override val id = R.id.FILTER_TAG_COMMAND
    @IgnoredOnParcel
    override val column = KEY_TAGID
    @IgnoredOnParcel
    override val title = R.string.tags

    companion object {
        fun fromStringExtra(extra: String) = parseStringExtra(extra)?.let {
            TagCriterion(it.first, *it.second)
        }
    }
}