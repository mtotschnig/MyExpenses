package org.totschnig.myexpenses.model2

import android.database.Cursor
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull

interface ICategoryInfo : Parcelable {
    val uuid: String
    val label: String
    val icon: String?
    val color: Int?
    val type: Int?
}

@Keep
@Parcelize
data class Category(
    val id: Long? = null,
    val parentId: Long? = null,
    val uuid: String? = null,
    val label: String = "",
    val icon: String? = null,
    val color: Int? = null,
    val type: Byte? = null,
) : Parcelable

@Keep
@Parcelize
@Serializable
data class CategoryInfo(
    override val uuid: String,
    override val label: String,
    override val icon: String? = null,
    override val color: Int? = null,
    override val type: Int? = null,
) : ICategoryInfo {
    companion object {
        fun fromCursor(cursor: Cursor): CategoryPath =
            cursor.asSequence.map {
                CategoryInfo(
                    it.getString(KEY_UUID),
                    it.getString(KEY_LABEL),
                    it.getStringOrNull(KEY_ICON),
                    it.getIntOrNull(KEY_COLOR),
                    if (it.getLongOrNull(KEY_PARENTID) == null)
                        it.getInt(KEY_TYPE) else null
                )
            }.toList().asReversed()
    }
}

typealias CategoryPath = List<CategoryInfo>

@Keep
@Parcelize
data class CategoryExport(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?,
    override val type: Int?,
    val children: List<CategoryExport>,
) : ICategoryInfo