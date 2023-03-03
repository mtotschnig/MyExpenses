package org.totschnig.myexpenses.sync.json

interface ICategoryInfo {
    val uuid: String
    val label: String
    val icon: String?
    val color: Int?
}

data class CategoryInfo(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?
) : ICategoryInfo

data class CategoryExport(
    override val uuid: String,
    override val label: String,
    override val icon: String?,
    override val color: Int?,
    val children: List<CategoryExport>
) : ICategoryInfo