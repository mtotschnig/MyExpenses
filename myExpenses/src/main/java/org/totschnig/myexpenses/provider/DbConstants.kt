package org.totschnig.myexpenses.provider

import org.totschnig.myexpenses.provider.DatabaseConstants.*

fun checkSealedWithAlias(baseTable: String, innerTable: String) =
    "max(" + checkForSealedAccount(
        baseTable,
        innerTable
    ) + ", " + checkForSealedDebt(baseTable) + ") AS " + KEY_SEALED

/**
 * we check if the object is linked to a sealed account, either via its account, it transfer_account, or its children.
 * For Children, we only need to check for transfer_account, since there account is identical to their parent.
 */
fun checkForSealedAccount(baseTable: String, innerTable: String) =
    "(SELECT max($KEY_SEALED) FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = $KEY_ACCOUNTID OR $KEY_ROWID = $KEY_TRANSFER_ACCOUNT OR $KEY_ROWID in (SELECT $KEY_TRANSFER_ACCOUNT FROM $innerTable WHERE $KEY_PARENTID = $baseTable.$KEY_ROWID))"

/**
 * we check if the object is linked to a sealed debt.
 * This can be used for queries that also include parts, where we do not need to include the parts here
 */
const val checkForSealedDebt =
    "coalesce((SELECT $KEY_SEALED FROM $TABLE_DEBTS WHERE $KEY_ROWID = $KEY_DEBT_ID), 0)"

/**
 * we check if the object (or any of its children) is linked to a sealed debt.
 */
fun checkForSealedDebt(baseTable: String) =
    "coalesce ((SELECT max($KEY_SEALED) FROM $TABLE_DEBTS WHERE $KEY_ROWID = $KEY_DEBT_ID OR $KEY_ROWID in (SELECT $KEY_DEBT_ID FROM $TABLE_TRANSACTIONS WHERE $KEY_PARENTID = $baseTable.$KEY_ROWID)), 0)"

fun categoryTreeSelect(
    sortOrder: String? = null,
    matches: String? = null,
    projection: String = "*",
    selection: String? = null,
    rootId: String? = null,
) = categoryTreeCTE(
    sortOrder = sortOrder,
    matches = matches,
    rootId = rootId
) + "SELECT $projection FROM Tree ${selection?.let { "WHERE $it" } ?: ""}"

fun categoryTreeWithMappedObjects(
    selection: String,
    projection: Array<String>,
    aggregate: Boolean
): String {
    fun wrapQuery(query: String, key: String, aggregate: Boolean) = query.let {
        if (aggregate) "sum($it)" else it
    } + " AS $key"

    fun subQuery(table: String, key: String, aggregate: Boolean) = wrapQuery(
        "(select 1 FROM $table WHERE $KEY_CATID IN (SELECT $KEY_ROWID FROM Tree))",
        key,
        aggregate
    )

    val map = projection.map {
        when (it) {
            KEY_MAPPED_TRANSACTIONS -> subQuery(TABLE_TRANSACTIONS, it, aggregate)
            KEY_MAPPED_TEMPLATES -> subQuery(TABLE_TEMPLATES, it, aggregate)
            KEY_MAPPED_BUDGETS -> subQuery(TABLE_BUDGET_CATEGORIES, it, aggregate)
            KEY_HAS_DESCENDANTS -> wrapQuery("(select count(*) FROM Tree) > 1", it, aggregate)
            else -> it
        }
    }
    return """
            ${categoryTreeCTE("$TABLE_CATEGORIES.$KEY_ROWID")}
            SELECT
            ${map.joinToString()}
            FROM $TABLE_CATEGORIES
            WHERE
            $selection
        """.trimIndent()
}

fun categoryTreeCTE(
    rootId: String? = null,
    sortOrder: String? = null,
    matches: String? = null
) =
    """
  WITH Tree as (
    SELECT
        $KEY_LABEL,
        $KEY_LABEL AS path,
        $KEY_COLOR,
        $KEY_ICON,
        $KEY_ROWID,
        $KEY_PARENTID,
        $KEY_USAGES,
        $KEY_LAST_USED,
        1 AS level,
        ${matches ?: "1"} AS matches
    FROM $TABLE_CATEGORIES main
    WHERE ${rootId?.let { " $KEY_ROWID = $it" } ?: "$KEY_PARENTID IS NULL"}
    UNION ALL
    SELECT
        subtree.$KEY_LABEL,
        Tree.path || ' > ' || subtree.$KEY_LABEL AS path,
        subtree.$KEY_COLOR,
        subtree.$KEY_ICON,
        subtree.$KEY_ROWID,
        subtree.$KEY_PARENTID,
        subtree.$KEY_USAGES,
        subtree.$KEY_LAST_USED,
        level + 1,
        ${matches ?: "1"} AS matches
    FROM categories subtree
    JOIN Tree ON Tree._id = subtree.parent_id
    ORDER BY level DESC${sortOrder?.let { ", $it" } ?: ""}
  )
""".trimIndent()