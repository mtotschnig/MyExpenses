package org.totschnig.myexpenses.sync.json

import androidx.annotation.Keep

interface ICategoryInfo {
    val uuid: String
    val label: String
    val icon: String?
    val color: Int?
    val type: Int?
}

@Keep
data class CategoryInfo(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?,
    override val type: Int? = null
) : ICategoryInfo

@Keep
data class CategoryExport(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?,
    override val type: Int?,
    val children: List<CategoryExport>
) : ICategoryInfo