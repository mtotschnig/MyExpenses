package org.totschnig.myexpenses.next.core.model.ledger

/**
 * The categories and tags of the user, loaded once for all features. Unlike the [Ledger] it is
 * available without consent, since it holds no transactions.
 *
 * @param categories without the split category
 * @param tags sorted by label
 */
data class Catalog(
    val categories: List<CategoryInfo> = emptyList(),
    val tags: List<TagInfo> = emptyList(),
) {
    /** Paths of the categories by id */
    val categoryPaths: Map<Long, String> by lazy { categories.associate { it.id to it.path } }

    val categoriesById: Map<Long, CategoryInfo> by lazy { categories.associateBy { it.id } }
}

/**
 * A category of the user
 *
 * @param path e.g. "Essen > Supermarkt"
 * @param isExpense / [isIncome] for which direction the category is meant, both for categories of both
 * @param mainId the main category it belongs to, its own id for a main category
 * @param parentId the category above, null for a main category
 */
data class CategoryInfo(
    val id: Long,
    val label: String,
    val path: String,
    val isExpense: Boolean = true,
    val isIncome: Boolean = true,
    val mainId: Long = id,
    val parentId: Long? = null,
)

/**
 * A tag of the user
 *
 * @param color as ARGB, null if the user chose none
 */
data class TagInfo(
    val id: Long,
    val label: String,
    val color: Int? = null,
)
