package org.totschnig.myexpenses.sync.json

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

interface ICategoryInfo: Parcelable {
    val uuid: String
    val label: String
    val icon: String?
    val color: Int?
    val type: Int?
}

@Keep
@Parcelize
data class CategoryInfo(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?,
    override val type: Int? = null
) : ICategoryInfo

@Keep
@Parcelize
data class CategoryExport(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?,
    override val type: Int?,
    val children: List<CategoryExport>
) : ICategoryInfo