package org.totschnig.myexpenses.provider.filter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
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
data class TagCriterion(
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

    override val displayInfo: DisplayInfo
        get() = TagCriterion

    companion object: DisplayInfo {
        fun fromStringExtra(extra: String) = parseStringExtra(extra)?.let {
            TagCriterion(it.first, *it.second)
        }

        override val title = R.string.tags
        override val extendedTitle = R.string.search_tag
        override val icon = Icons.Default.Tag
        override val isPartial = true
        override val clazz = TagCriterion::class
    }
}