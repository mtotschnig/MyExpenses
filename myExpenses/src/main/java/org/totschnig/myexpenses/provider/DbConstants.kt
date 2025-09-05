package org.totschnig.myexpenses.provider

import android.net.Uri
import android.text.TextUtils
import androidx.core.text.isDigitsOnly
import org.totschnig.myexpenses.db2.DEFAULT_CATEGORY_PATH_SEPARATOR
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.db2.asCategoryType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.CTE_TABLE_NAME_FULL_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_NEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET_ROLLOVER_PREVIOUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY_OTHER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY_SELF
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DISPLAY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_INCOME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_TRANSFERS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_CLEARED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_DESCENDANTS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_FUTURE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_TRANSFERS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_ASSET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LATEST_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LATEST_EXCHANGE_RATE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LEVEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_BUDGETS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_METHODS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_PAYEES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MATCHES_FILTER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ONE_TIME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SHORT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_BY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_DIRECTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUPPORTS_RECONCILIATION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USAGES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VISIBLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_ROW_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_EXCHANGE_RATES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_FLAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_TYPES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_BUDGET_ALLOCATIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_EQUIVALENT_AMOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PLAN_INSTANCE_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PRICES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES_TAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS_TAGS
import org.totschnig.myexpenses.provider.DatabaseConstants.TREE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_PRIORITIZED_PRICES
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_WITH_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_ARCHIVED
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_INCLUDE_ALL
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_TRANSACTION_ID_LIST
import org.totschnig.myexpenses.provider.filter.Criterion

private fun requireIdParameter(parameter: String) {
    require(parameter.isDigitsOnly())
}

/**
 * - with parameter QUERY_PARAMETER_TRANSACTION_ID_LIST return requested ids
 * - with parameter KEY_TRANSACTIONID return the transaction
 *   (used from Transaction Detail Fragment)
 * - with parameter KEY_PARENTID return just the split parts
 *   (used from Edit split)
 * - otherwise accountSelector logic is applied
 */
fun Uri.transactionQuerySelector(table: String) =
    getQueryParameter(QUERY_PARAMETER_TRANSACTION_ID_LIST)?.let { idList ->
        idList.split(',').forEach { requireIdParameter(it.trim()) }
        "$KEY_ROWID IN ($idList)"
    } ?: getQueryParameter(KEY_TRANSACTIONID)?.let {
        requireIdParameter(it)
        "$KEY_ROWID = $it"
    } ?: getQueryParameter(KEY_PARENTID)?.let {
        requireIdParameter(it)
        "$KEY_PARENTID = $it"
    } ?: accountSelector.let { if (it.isEmpty()) it else "$table.$it" }


val Uri.templateQuerySelector: String?
    get() = getQueryParameter(KEY_PARENTID)?.let {
        requireIdParameter(it)
        "$KEY_PARENTID = $it"
    }

/**
 * with parameter KEY_ACCOUNTID show single account, with parameter KEY_CURRENCY show for all
 * accounts with given currency (if not excluded from totals), otherwise all transactions (if not
 * excluded from totals) = Grand total account. With include_all passed in exclude from totals is ignored
 */

val Uri.accountSelector: String
    get() = if (getBooleanQueryParameter(QUERY_PARAMETER_INCLUDE_ALL, false)) ""
    else KEY_ACCOUNTID + (
            getQueryParameter(KEY_ACCOUNTID)?.let {
                requireIdParameter(it)
                "= $it"
            } ?: (" IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_EXCLUDE_FROM_TOTALS=0 " +
                    (getQueryParameter(KEY_CURRENCY)?.let {
                        "AND $KEY_CURRENCY = '$it'"
                    } ?: "") + ")")
            )

fun checkSealedWithAlias(baseTable: String) =
    "max(" + checkForSealedAccount(
        baseTable,
        TABLE_TRANSACTIONS
    ) + ", " + checkForSealedDebt(baseTable) + ") AS " + KEY_SEALED

/**
 * we check if the object is linked to a sealed account, either via its account, it transfer_account, or its children.
 * For Children, we only need to check for transfer_account, since there account is identical to their parent.
 */
fun checkForSealedAccount(baseTable: String, innerTable: String, withTransfer: Boolean = true) =
    if (withTransfer)
        "(SELECT max($KEY_SEALED) FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = $baseTable.$KEY_ACCOUNTID OR $KEY_ROWID = $KEY_TRANSFER_ACCOUNT OR $KEY_ROWID in (SELECT $KEY_TRANSFER_ACCOUNT FROM $innerTable WHERE $KEY_PARENTID = $baseTable.$KEY_ROWID))"
    else "(SELECT $KEY_SEALED FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = $KEY_ACCOUNTID)"

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
    projection: Array<String>? = null,
    selection: String? = null,
    rootExpression: String? = null,
    categorySeparator: String? = null,
    typeParameter: String? = null,
) = "WITH " + categoryTreeCTE(
    rootExpression = rootExpression,
    sortOrder = sortOrder,
    matches = matches,
    categorySeparator = categorySeparator,
    type = typeParameter?.toByte()
) + "SELECT ${projection?.joinToString() ?: "*"} FROM Tree ${selection?.let { "WHERE $it" } ?: ""}"

const val categoryTreeSelectForTrigger = """
WITH Tree AS (
SELECT
    $KEY_ROWID,
    $KEY_PARENTID,
    1 AS $KEY_LEVEL
FROM $TABLE_CATEGORIES main
WHERE $KEY_ROWID= new.$KEY_ROWID
UNION ALL
SELECT
    subtree.$KEY_ROWID,
    subtree.$KEY_PARENTID,
    level + 1
FROM $TABLE_CATEGORIES subtree
JOIN Tree ON Tree.$KEY_ROWID = subtree.$KEY_PARENTID
ORDER BY $KEY_LEVEL DESC
) SELECT $KEY_ROWID From Tree
"""

fun budgetColumn(year: String?, second: String?, withAlias: Boolean = true): String {
    val mainSelect = subSelectFromAllocations(
        KEY_BUDGET,
        year,
        second,
        false
    )
    return (if (year == null) mainSelect else "coalesce($mainSelect," +
            "(SELECT $KEY_BUDGET from Allocations WHERE $KEY_ONE_TIME = 0 AND (coalesce($KEY_YEAR,0) < $year ${second?.let { " OR (coalesce($KEY_YEAR,0) = $year AND coalesce($KEY_SECOND_GROUP,0) < $it)" } ?: ""}) ORDER BY $KEY_YEAR DESC ${if (second == null) "" else ", $KEY_SECOND_GROUP DESC"} LIMIT 1))") +
            if (withAlias) " AS $KEY_BUDGET" else ""
}

fun subSelectFromAllocations(
    column: String,
    year: String?,
    second: String?,
    withAlias: Boolean = true,
) =
    "(SELECT $column from Allocations ${budgetSelectForGroup(year, second)})" +
            if (withAlias) " AS $column" else ""

fun categoryTreeWithSum(
    aggregateFunction: String,
    homeCurrency: String,
    sortOrder: String? = null,
    selection: String? = null,
    projection: Array<String>,
    uri: Uri,
): String {
    val year = uri.getQueryParameter(KEY_YEAR)
    val second = uri.getQueryParameter(KEY_SECOND_GROUP)
    val accountSelector = uri.accountSelector
    val incomeType = uri.getBooleanQueryParameter(KEY_TYPE, false)
    val type = incomeType.asCategoryType
    val aggregateNeutral = uri.getBooleanQueryParameter(QUERY_PARAMETER_AGGREGATE_NEUTRAL, false)
    val map = projection.map {
        when (it) {
            KEY_SUM -> buildString {
                val amountStatement = if (aggregateNeutral) KEY_DISPLAY_AMOUNT else
                //the ELSE in the CASE statement is FLAG_NEUTRAL because the categoryTreeCTE
                // returns categories which are either the requested type or neutral
                    "CASE $KEY_TYPE WHEN $type THEN $KEY_DISPLAY_AMOUNT ELSE ${if (incomeType) "max" else "min"}($KEY_DISPLAY_AMOUNT, 0) END"
                append("(SELECT $aggregateFunction($amountStatement) FROM $CTE_TRANSACTION_AMOUNTS ")
                append(") AS $KEY_SUM")
            }

            KEY_BUDGET -> budgetColumn(year, second)
            KEY_BUDGET_ROLLOVER_NEXT, KEY_BUDGET_ROLLOVER_PREVIOUS, KEY_ONE_TIME ->
                subSelectFromAllocations(it, year, second)

            else -> it
        }
    }
    val forHome =
        uri.getQueryParameter(KEY_ACCOUNTID) == null && uri.getQueryParameter(KEY_CURRENCY) == null
    return buildString {
        append("WITH ")
        append(buildSearchCte(VIEW_WITH_ACCOUNT, homeCurrency, forHome))
        append(",")
        append(
            categoryTreeCTE(
                sortOrder = sortOrder,
                type = type
            )
        )
        append(", $CTE_TRANSACTION_AMOUNTS AS (SELECT $KEY_DISPLAY_AMOUNT FROM $CTE_SEARCH")
        append(" WHERE ")
        append(WHERE_NOT_VOID)
        append(" AND +$CTE_SEARCH.$accountSelector")
        selection?.takeIf { it.isNotEmpty() }?.let {
            append(" AND $it")
        }
        append(" AND $KEY_CATID = $TREE_CATEGORIES.${KEY_ROWID}")
        append(")")
        if (projection.contains(KEY_BUDGET)) {
            val budgetId = uri.getQueryParameter(KEY_BUDGETID)!!
            require(budgetId.isDigitsOnly())
            append(", ")
            append(budgetAllocationsCTE("$KEY_CATID= Tree.$KEY_ROWID AND $KEY_BUDGETID = $budgetId"))
        }
        append(" SELECT ${map.joinToString()} FROM Tree")
    }
}

private fun budgetAllocationsCTE(budgetSelect: String) =
    "Allocations AS (SELECT $KEY_BUDGET, $KEY_YEAR, $KEY_SECOND_GROUP, $KEY_ONE_TIME, $KEY_BUDGET_ROLLOVER_PREVIOUS, $KEY_BUDGET_ROLLOVER_NEXT FROM $TABLE_BUDGET_ALLOCATIONS WHERE $budgetSelect)"

fun parseBudgetCategoryUri(uri: Uri) = uri.pathSegments.let { it[1] to it[2] }

fun budgetSelect(uri: Uri) = with(parseBudgetCategoryUri(uri)) {
    "$KEY_CATID ${"= $second"} AND $KEY_BUDGETID = $first"
}

fun budgetSelectForGroup(year: String?, second: String?) =
    if (year == null) "" else "WHERE $KEY_YEAR = $year ${second?.let { "AND $KEY_SECOND_GROUP = $it" } ?: ""}"

/**
 * select the budget for a period + the sum of all rollOvers
 */
fun totalBudgetAllocation(uri: Uri): String {
    val budgetId = uri.pathSegments[1]
    val year = uri.getQueryParameter(KEY_YEAR)
    val second = uri.getQueryParameter(KEY_SECOND_GROUP)

    return """WITH AllAllocations AS (SELECT $KEY_CATID, $KEY_BUDGET, $KEY_YEAR, $KEY_SECOND_GROUP, $KEY_ONE_TIME, $KEY_BUDGET_ROLLOVER_PREVIOUS, $KEY_BUDGET_ROLLOVER_NEXT FROM $TABLE_BUDGET_ALLOCATIONS WHERE budget_id = $budgetId),
 Allocations AS (SELECT * FROM AllAllocations WHERE $KEY_CATID = 0)
        SELECT ${budgetColumn(year, second, false)} + coalesce((SELECT sum($KEY_BUDGET_ROLLOVER_PREVIOUS) FROM AllAllocations WHERE $KEY_YEAR = $year AND $KEY_SECOND_GROUP = $second),0)"""
}

fun budgetAllocation(uri: Uri): String {
    val budgetId = uri.pathSegments[1]
    val year = uri.getQueryParameter(KEY_YEAR)
    val second = uri.getQueryParameter(KEY_SECOND_GROUP)
    val cte = """WITH Allocations AS (SELECT $KEY_CATID, $KEY_BUDGET, $KEY_YEAR, $KEY_SECOND_GROUP, $KEY_ONE_TIME, $KEY_BUDGET_ROLLOVER_PREVIOUS, $KEY_BUDGET_ROLLOVER_NEXT FROM $TABLE_BUDGET_ALLOCATIONS WHERE budget_id = $budgetId AND $KEY_CATID = 0),
 AllocationsForPeriod AS (SELECT * FROM Allocations WHERE $KEY_YEAR = $year AND $KEY_SECOND_GROUP = $second)
    """
    fun subSelectForPeriod(column: String) = "(SELECT $column from AllocationsForPeriod) AS $column"
    return "$cte SELECT " +
            budgetColumn(year, second) + "," +
            subSelectForPeriod(KEY_BUDGET_ROLLOVER_PREVIOUS) + "," +
            subSelectForPeriod(KEY_BUDGET_ROLLOVER_NEXT) + "," +
            subSelectForPeriod(KEY_ONE_TIME)
}

fun categoryTreeWithMappedObjects(
    selection: String,
    projection: Array<String>,
    aggregate: Boolean,
): String {
    fun wrapQuery(query: String, key: String, aggregate: Boolean) = query.let {
        if (aggregate) "sum($it)" else it
    } + " AS $key"

    fun subQuery(table: String, key: String, aggregate: Boolean) = wrapQuery(
        "(select 1 FROM $table WHERE $KEY_CATID IN (SELECT $KEY_ROWID FROM $TREE_CATEGORIES))",
        key,
        aggregate
    )

    val map = projection.map {
        when (it) {
            KEY_MAPPED_TRANSACTIONS -> subQuery(TABLE_TRANSACTIONS, it, aggregate)
            KEY_MAPPED_TEMPLATES -> subQuery(TABLE_TEMPLATES, it, aggregate)
            KEY_MAPPED_BUDGETS -> subQuery(TABLE_BUDGET_ALLOCATIONS, it, aggregate)
            KEY_HAS_DESCENDANTS -> wrapQuery(
                "(select count(*) FROM $TREE_CATEGORIES) > 1",
                it,
                aggregate
            )

            else -> it
        }
    }
    return """WITH ${categoryTreeCTE(rootExpression = "$KEY_ROWID = $TABLE_CATEGORIES.$KEY_ROWID")}
            SELECT
            ${map.joinToString()}
            FROM $TABLE_CATEGORIES
            WHERE
            $selection
        """.trimIndent()
}

fun labelEscapedForQif(tableName: String) =
    "replace(replace($tableName.$KEY_LABEL,'/','\\u002F'), ':','\\u003A')"

fun maybeEscapeLabel(categorySeparator: String?, tableName: String) =
    if (categorySeparator == ":") labelEscapedForQif(tableName) else "$tableName.$KEY_LABEL"

const val CATEGORY_SEPARATOR = '>'

@JvmOverloads
fun getCategoryTreeForView(
    rootExpression: String = "$KEY_PARENTID IS NULL",
    withRootLabel: Boolean = true,
): String {
    val rootPath = if (withRootLabel) "main.$KEY_LABEL" else "''"
    val separator = if (withRootLabel) "' $CATEGORY_SEPARATOR '" else
        "CASE WHEN Tree.$KEY_PATH = '' THEN '' ELSE ' $CATEGORY_SEPARATOR ' END"
    return """
WITH Tree AS (
SELECT
    $rootPath AS $KEY_PATH,
    $KEY_ICON,
    $KEY_TYPE,
    $KEY_ROWID
FROM $TABLE_CATEGORIES main
WHERE $rootExpression
UNION ALL
SELECT
    Tree.$KEY_PATH || $separator || subtree.$KEY_LABEL,
    subtree.$KEY_ICON,
    subtree.$KEY_TYPE,
    subtree.$KEY_ROWID
FROM $TABLE_CATEGORIES subtree
JOIN Tree ON Tree.$KEY_ROWID = subtree.$KEY_PARENTID
)
""".trimIndent()
}

fun getPayeeWithDuplicatesCTE(selection: String?, collate: String) = """
    WITH cte AS (SELECT ${
    BaseTransactionProvider.payeeProjection(TABLE_PAYEES).joinToString(",")
}, 1 AS $KEY_LEVEL FROM $TABLE_PAYEES 
    WHERE $KEY_PARENTID IS NULL AND $KEY_ROWID != $NULL_ROW_ID ${selection?.let { " AND $it" } ?: ""}
    UNION ALL
    SELECT ${
    BaseTransactionProvider.payeeProjection("dups").joinToString(",")
}, $KEY_LEVEL+1 from $TABLE_PAYEES dups
    JOIN cte ON cte.$KEY_ROWID = dups.$KEY_PARENTID ORDER BY $KEY_LEVEL DESC, $KEY_PAYEE_NAME COLLATE $collate) SELECT * FROM cte
""".trimIndent()

/**
 * if [rootExpression] is specified [type] is ignored
 */
fun categoryTreeCTE(
    rootExpression: String? = null,
    sortOrder: String? = null,
    matches: String? = null,
    categorySeparator: String? = null,
    type: Byte? = null,
): String {
    val where = rootExpression ?: buildString {
        append("$KEY_PARENTID IS NULL")
        if (type != null) {
            append(" AND $KEY_TYPE  ")
            append(
                if (type == 0.toByte()) "= 0"
                else "& $type > 0"
            )
        }
    }
    return """Tree AS (
SELECT
    $KEY_LABEL,
    $KEY_UUID,
    ${maybeEscapeLabel(categorySeparator, "main")} AS $KEY_PATH,
    $KEY_COLOR,
    $KEY_ICON,
    $KEY_ROWID,
    $KEY_PARENTID,
    $KEY_USAGES,
    $KEY_LAST_USED,
    $KEY_TYPE,
    1 AS $KEY_LEVEL,
    ${matches?.replace("_Tree_", "main") ?: "1"} AS $KEY_MATCHES_FILTER
FROM $TABLE_CATEGORIES main
WHERE $where
UNION ALL
SELECT
    subtree.$KEY_LABEL,
    subtree.$KEY_UUID,
    Tree.$KEY_PATH || '${categorySeparator ?: DEFAULT_CATEGORY_PATH_SEPARATOR}' || ${
        maybeEscapeLabel(
            categorySeparator,
            "subtree"
        )
    },
    subtree.$KEY_COLOR,
    subtree.$KEY_ICON,
    subtree.$KEY_ROWID,
    subtree.$KEY_PARENTID,
    subtree.$KEY_USAGES,
    subtree.$KEY_LAST_USED,
    subtree.$KEY_TYPE,
    level + 1,
    ${matches?.replace("_Tree_", "subtree") ?: "1"} AS $KEY_MATCHES_FILTER
FROM $TABLE_CATEGORIES subtree
JOIN Tree ON Tree._id = subtree.parent_id
ORDER BY $KEY_LEVEL DESC${sortOrder?.let { ", $it" } ?: ""}
)
""".trimIndent()
}

fun fullCatCase(categorySeparator: String?) = "(" + categoryTreeSelect(
    projection = arrayOf(KEY_PATH),
    selection = "$KEY_ROWID = $KEY_CATID",
    categorySeparator = categorySeparator
) + ")"

fun categoryPathFromLeave(rowId: String): String {
    check(rowId.toInt() > 0) { "rowId must be positive" }
    return """
    WITH Tree AS (SELECT $KEY_PARENTID, $KEY_LABEL, $KEY_ICON, $KEY_UUID, $KEY_COLOR, $KEY_TYPE  from $TABLE_CATEGORIES child where $KEY_ROWID = $rowId
    UNION ALL
    SELECT parent.$KEY_PARENTID, parent.$KEY_LABEL, parent.$KEY_ICON, parent.$KEY_UUID, parent.$KEY_COLOR, parent.$KEY_TYPE from $TABLE_CATEGORIES parent JOIN Tree on Tree.$KEY_PARENTID = parent.$KEY_ROWID
    ) SELECT * FROM Tree
""".trimIndent()
}

const val TRANSFER_ACCOUNT_LABEL =
    "CASE WHEN $KEY_TRANSFER_ACCOUNT THEN (SELECT $KEY_LABEL FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = $KEY_TRANSFER_ACCOUNT) END AS $KEY_TRANSFER_ACCOUNT_LABEL"


fun accountQueryCTE(
    homeCurrency: String,
    endOfDay: Boolean,
    aggregateFunction: String,
    typeWithFallBack: String,
    date: String,
    dynamicExpression: String,
    aggregateInvisible: Boolean,
): String {
    val dateCriterion =
        if (endOfDay) "'$date', 'localtime', 'start of day', '+1 day', '-1 second', 'utc'" else "'$date'"
    val dateCriterionForPricesTable = if (date == "now") "date()" else "'$date'"
    val isExpense =
        "$KEY_TYPE = $FLAG_EXPENSE OR ($KEY_TYPE = $FLAG_NEUTRAL AND $KEY_AMOUNT < 0)"
    val isIncome =
        "$KEY_TYPE = $FLAG_INCOME OR ($KEY_TYPE = $FLAG_NEUTRAL AND $KEY_AMOUNT > 0)"
    val isTransfer = "$KEY_TYPE = $FLAG_TRANSFER"

    val invisibleFilter = if (aggregateInvisible) "" else " WHERE $KEY_VISIBLE = 1"

    val fullAccountProjection = arrayOf(
        "CASE WHEN $dynamicExpression THEN $CTE_LATEST_RATES.$KEY_VALUE END AS $KEY_LATEST_EXCHANGE_RATE ",
        "CASE WHEN $dynamicExpression THEN $CTE_LATEST_RATES.$KEY_DATE END AS $KEY_LATEST_EXCHANGE_RATE_DATE",
        KEY_EXCHANGE_RATE,
        "$TABLE_ACCOUNTS.$KEY_ROWID AS $KEY_ROWID",
        "$TABLE_ACCOUNTS.$KEY_LABEL",
        "$TABLE_ACCOUNTS.$KEY_DESCRIPTION AS $KEY_DESCRIPTION",
        KEY_OPENING_BALANCE,
        "CASE WHEN $KEY_CURRENCY = '$homeCurrency' THEN $KEY_OPENING_BALANCE ELSE $KEY_OPENING_BALANCE * $KEY_EXCHANGE_RATE END AS $KEY_EQUIVALENT_OPENING_BALANCE",
        "$TABLE_ACCOUNTS.$KEY_CURRENCY AS $KEY_CURRENCY",
        KEY_COLOR,
        "$TABLE_ACCOUNTS.$KEY_GROUPING AS $KEY_GROUPING",
        "$TABLE_ACCOUNT_TYPES.$KEY_LABEL AS $KEY_ACCOUNT_TYPE_LABEL",
        KEY_IS_ASSET,
        KEY_SUPPORTS_RECONCILIATION,
        KEY_TYPE,
        KEY_TYPE_SORT_KEY,
        KEY_FLAG,
        KEY_FLAG_LABEL,
        KEY_VISIBLE,
        KEY_FLAG_SORT_KEY,
        KEY_FLAG_ICON,
        KEY_SORT_KEY,
        KEY_EXCLUDE_FROM_TOTALS,
        KEY_SYNC_ACCOUNT_NAME,
        KEY_UUID,
        KEY_SORT_BY,
        KEY_SORT_DIRECTION,
        KEY_CRITERION,
        KEY_SEALED,
        "$KEY_OPENING_BALANCE + coalesce($KEY_CURRENT,0) AS $KEY_CURRENT_BALANCE",
        "($KEY_OPENING_BALANCE + coalesce($KEY_CURRENT,0)) * CASE WHEN $KEY_CURRENCY = '$homeCurrency' THEN 1 WHEN $dynamicExpression THEN coalesce($CTE_LATEST_RATES.$KEY_VALUE,$KEY_EXCHANGE_RATE) ELSE $KEY_EXCHANGE_RATE END AS $KEY_EQUIVALENT_CURRENT_BALANCE",
        KEY_SUM_INCOME,
        KEY_SUM_EXPENSES,
        KEY_SUM_TRANSFERS,
        KEY_EQUIVALENT_INCOME,
        KEY_EQUIVALENT_EXPENSES,
        KEY_EQUIVALENT_TRANSFERS,
        "$KEY_OPENING_BALANCE + coalesce($KEY_TOTAL,0) AS $KEY_TOTAL",
        "($KEY_OPENING_BALANCE + coalesce($KEY_TOTAL,0)) * CASE WHEN $KEY_CURRENCY = '$homeCurrency' THEN 1 WHEN $dynamicExpression THEN coalesce($CTE_LATEST_RATES.$KEY_VALUE,$KEY_EXCHANGE_RATE) ELSE $KEY_EXCHANGE_RATE END AS $KEY_EQUIVALENT_TOTAL",
        "$KEY_OPENING_BALANCE + coalesce($KEY_CLEARED_TOTAL,0) AS $KEY_CLEARED_TOTAL",
        "$KEY_OPENING_BALANCE + coalesce($KEY_RECONCILED_TOTAL,0) AS $KEY_RECONCILED_TOTAL",
        KEY_USAGES,
        KEY_HAS_FUTURE,
        KEY_HAS_CLEARED,
        KEY_LAST_USED,
        KEY_BANK_ID,
        "$KEY_CURRENCY != '$homeCurrency' AND $dynamicExpression AS $KEY_DYNAMIC"
    )
    return """
WITH now as (
    SELECT
        cast(strftime('%s', $dateCriterion) as integer) AS now
), $CTE_LATEST_RATES as (
  SELECT p.$KEY_COMMODITY, p.$KEY_VALUE, p.$KEY_DATE
  FROM $VIEW_PRIORITIZED_PRICES p
  WHERE p.$KEY_CURRENCY = '$homeCurrency'
  AND p.$KEY_DATE = (
      -- Get the most recent date per commodity
      SELECT MAX(p2.$KEY_DATE)
      FROM $TABLE_PRICES p2
      WHERE p2.$KEY_COMMODITY = p.$KEY_COMMODITY AND p2.$KEY_CURRENCY = '$homeCurrency' AND $KEY_DATE <= $dateCriterionForPricesTable
  )
), base AS (SELECT $VIEW_WITH_ACCOUNT.*, $KEY_EQUIVALENT_AMOUNT, $KEY_EXCHANGE_RATE FROM
    ${exchangeRateJoin(VIEW_WITH_ACCOUNT, KEY_ACCOUNTID, homeCurrency)}
    ${equivalentAmountJoin(homeCurrency)}
), amounts AS (
    SELECT
        $KEY_AMOUNT,
        $typeWithFallBack AS $KEY_TYPE,
        $KEY_TRANSFER_PEER,
        $KEY_CR_STATUS,
        $KEY_DATE,
        CASE WHEN $KEY_CURRENCY != '$homeCurrency'
          THEN
            coalesce(
              CASE WHEN $KEY_DYNAMIC
                THEN ${calcEquivalentAmountForSplitParts("base")}
              END,
              coalesce($KEY_EXCHANGE_RATE, 1) * $KEY_AMOUNT
            )
          ELSE $KEY_AMOUNT
          END AS $KEY_EQUIVALENT_AMOUNT,
        $KEY_ACCOUNTID
    FROM base
    WHERE $WHERE_NOT_SPLIT AND $WHERE_NOT_ARCHIVED AND $KEY_CR_STATUS != '${CrStatus.VOID.name}'
), aggregates AS (
    SELECT
        $KEY_ACCOUNTID,
        $aggregateFunction($KEY_AMOUNT) as $KEY_TOTAL,
        $aggregateFunction(CASE WHEN $isIncome THEN $KEY_AMOUNT ELSE 0 END) as $KEY_SUM_INCOME,
        $aggregateFunction(CASE WHEN ($isIncome) AND $KEY_TRANSFER_PEER IS NULL THEN $KEY_EQUIVALENT_AMOUNT ELSE 0 END) as $KEY_EQUIVALENT_INCOME,
        $aggregateFunction(CASE WHEN $isExpense THEN $KEY_AMOUNT ELSE 0 END) as $KEY_SUM_EXPENSES,
        $aggregateFunction(CASE WHEN ($isExpense) AND $KEY_TRANSFER_PEER IS NULL THEN $KEY_EQUIVALENT_AMOUNT ELSE 0 END) as $KEY_EQUIVALENT_EXPENSES,
        $aggregateFunction(CASE WHEN $isTransfer THEN $KEY_AMOUNT ELSE 0  END) as $KEY_SUM_TRANSFERS,
        $aggregateFunction(CASE WHEN ($isTransfer) AND $KEY_TRANSFER_PEER IS NULL THEN $KEY_EQUIVALENT_AMOUNT ELSE 0  END) as $KEY_EQUIVALENT_TRANSFERS,
        $aggregateFunction(CASE WHEN $KEY_DATE <= (select now from now) THEN $KEY_AMOUNT ELSE 0 END) as $KEY_CURRENT,
        $aggregateFunction(CASE WHEN $KEY_CR_STATUS IN ( 'RECONCILED', 'CLEARED' ) THEN $KEY_AMOUNT ELSE 0 END) as $KEY_CLEARED_TOTAL,
        $aggregateFunction(CASE WHEN $KEY_CR_STATUS = 'RECONCILED' THEN $KEY_AMOUNT ELSE 0 END) as $KEY_RECONCILED_TOTAL,
        max(CASE WHEN $KEY_CR_STATUS = 'CLEARED' THEN 1 ELSE 0 END) as $KEY_HAS_CLEARED,
        max($KEY_DATE) > (select now from now) as $KEY_HAS_FUTURE
   from amounts group by $KEY_ACCOUNTID
), $CTE_TABLE_NAME_FULL_ACCOUNTS AS (
    SELECT ${fullAccountProjection.joinToString()}
    FROM $accountWithTypeAndFlag LEFT JOIN aggregates ON $TABLE_ACCOUNTS.$KEY_ROWID = aggregates.$KEY_ACCOUNTID LEFT JOIN $CTE_LATEST_RATES ON $TABLE_ACCOUNTS.$KEY_CURRENCY = $CTE_LATEST_RATES.$KEY_COMMODITY  
    ${exchangeRateJoin("", KEY_ROWID, homeCurrency, TABLE_ACCOUNTS)} $invisibleFilter
)
"""
}

const val accountWithTypeAndFlag = "$TABLE_ACCOUNTS LEFT JOIN $TABLE_ACCOUNT_TYPES ON $KEY_TYPE = $TABLE_ACCOUNT_TYPES.$KEY_ROWID LEFT JOIN $TABLE_ACCOUNT_FLAGS ON $KEY_FLAG = $TABLE_ACCOUNT_FLAGS.$KEY_ROWID"

fun exchangeRateJoin(
    table: String,
    colum: String,
    homeCurrency: String,
    joinTable: String = table,
) =
    " $table LEFT JOIN $TABLE_ACCOUNT_EXCHANGE_RATES ON $joinTable.$colum = $TABLE_ACCOUNT_EXCHANGE_RATES.$KEY_ACCOUNTID AND $KEY_CURRENCY_SELF = $joinTable.$KEY_CURRENCY AND $KEY_CURRENCY_OTHER = '$homeCurrency'"

@JvmOverloads
fun equivalentAmountJoin(
    homeCurrency: String,
    idColumn: String = KEY_ROWID,
) =
    " LEFT JOIN $TABLE_EQUIVALENT_AMOUNTS ON $idColumn = $TABLE_EQUIVALENT_AMOUNTS.$KEY_TRANSACTIONID AND $TABLE_EQUIVALENT_AMOUNTS.$KEY_CURRENCY = '$homeCurrency'"

fun transactionMappedObjectQuery(selection: String): String = """
with data as
 (select $KEY_ROWID, $KEY_CATID, $KEY_METHODID, $KEY_PAYEEID, $KEY_TRANSFER_ACCOUNT, $KEY_TAGID from $TABLE_TRANSACTIONS left join $TABLE_TRANSACTIONS_TAGS on $KEY_TRANSACTIONID = $KEY_ROWID where $KEY_CR_STATUS != '${CrStatus.VOID.name}' AND +$selection)
 SELECT
       exists(select 1 from data) AS $KEY_COUNT,
       exists(select 1 from data where $KEY_CATID > 0) AS $KEY_MAPPED_CATEGORIES,
       exists(select 1 from data where $KEY_METHODID > 0) AS $KEY_MAPPED_METHODS,
       exists(select 1 from data where $KEY_PAYEEID > 0) AS $KEY_MAPPED_PAYEES,
       exists(select 1 from data where $KEY_TRANSFER_ACCOUNT > 0) AS $KEY_HAS_TRANSFERS,
       exists(select 1 from data where $KEY_TAGID is not null) AS $KEY_MAPPED_TAGS
""".trimIndent()

const val TAG_LIST_EXPRESSION = "group_concat($KEY_TAGID,'') AS $KEY_TAGLIST"

fun tagJoin(mainTable: String): String {
    val (joinTable, referenceColumn) = when (mainTable) {
        TABLE_TRANSACTIONS -> TABLE_TRANSACTIONS_TAGS to KEY_TRANSACTIONID
        TABLE_TEMPLATES -> TABLE_TEMPLATES_TAGS to KEY_TEMPLATEID
        else -> throw IllegalArgumentException()
    }
    return associativeJoin(mainTable, joinTable, TABLE_TAGS, referenceColumn, KEY_TAGID)
}

fun associativeJoin(
    mainTable: String,
    joinTable: String,
    associatedTable: String,
    mainColumn: String,
    associateColumn: String,
) =
    " LEFT JOIN $joinTable ON $joinTable.$mainColumn = $mainTable.$KEY_ROWID LEFT JOIN $associatedTable ON $associateColumn= $associatedTable.$KEY_ROWID"

fun tagGroupBy(tableName: String): String =
    " GROUP BY $tableName.$KEY_ROWID"

fun buildTransactionRowSelect(filter: Criterion?) =
    "SELECT $KEY_ROWID from $TABLE_TRANSACTIONS WHERE $KEY_ACCOUNTID = ?" +
            if (filter != null) {
                " AND ${filter.getSelectionForParents(TABLE_TRANSACTIONS)}"
            } else ""

fun transactionListAsCTE(catId: String, forHome: String?) =
    getCategoryTreeForView("$KEY_ROWID = $catId", false) +
            ", $CTE_SEARCH AS (" +
            transactionsJoin(withDisplayAmount = true, forHome = forHome) +
            " WHERE $KEY_STATUS != $STATUS_UNCOMMITTED " +
            tagGroupBy(TABLE_TRANSACTIONS) +
            ")"

fun buildViewDefinition(tableName: String) =
    " AS ${getCategoryTreeForView()} ${transactionsJoin(tableName, false)}"

private fun transactionsJoin(
    tableName: String = TABLE_TRANSACTIONS,
    withPlanInstance: Boolean = tableName == TABLE_TRANSACTIONS,
    withDisplayAmount: Boolean = false,
    forHome: String? = null,
) = buildString {
    append(" SELECT $tableName.*, Tree.$KEY_PATH, Tree.$KEY_ICON, Tree.$KEY_TYPE, $TABLE_PAYEES.$KEY_PAYEE_NAME, $TABLE_PAYEES.$KEY_SHORT_NAME, $TABLE_METHODS.$KEY_LABEL AS $KEY_METHOD_LABEL, $TABLE_METHODS.$KEY_ICON AS $KEY_METHOD_ICON")
    if (withPlanInstance) {
        append(", $TABLE_PLAN_INSTANCE_STATUS.$KEY_TEMPLATEID")
    }
    if (withDisplayAmount) {
        append(", ${getAmountCalculation(forHome, tableName, TABLE_ACCOUNTS)} AS $KEY_DISPLAY_AMOUNT")
    }
    append(", $TAG_LIST_EXPRESSION")
    append(", $TABLE_ACCOUNTS.$KEY_CURRENCY")
    append(
        """ FROM $tableName
        | LEFT JOIN $TABLE_PAYEES ON $KEY_PAYEEID = $TABLE_PAYEES.$KEY_ROWID
        | LEFT JOIN $TABLE_METHODS ON $KEY_METHODID = $TABLE_METHODS.$KEY_ROWID
        | LEFT JOIN $TABLE_ACCOUNTS ON $tableName.$KEY_ACCOUNTID = $TABLE_ACCOUNTS.$KEY_ROWID
        | LEFT JOIN Tree ON $KEY_CATID = TREE.$KEY_ROWID""".trimMargin()
    )
    if (withPlanInstance) {
        append(" LEFT JOIN $TABLE_PLAN_INSTANCE_STATUS ON $tableName.$KEY_ROWID = $TABLE_PLAN_INSTANCE_STATUS.$KEY_TRANSACTIONID")
    }
    append(tagJoin(tableName))
    if (forHome != null) {
        append(
            exchangeRateJoin(
                table = "",
                colum = KEY_ROWID,
                homeCurrency = forHome,
                joinTable = TABLE_ACCOUNTS
            )
        )
        append(equivalentAmountJoin(forHome, "$tableName.$KEY_ROWID"))
    }

}

const val CTE_TRANSACTION_GROUPS = "cte_transaction_groups"
const val CTE_TRANSACTION_AMOUNTS = "cte_amounts"
const val CTE_SEARCH = "cte_search"
const val CTE_LATEST_RATES = "cte_latest_rates"

fun buildSearchCte(
    forTable: String,
    homeCurrency: String,
    forHome: Boolean,
) = "$CTE_SEARCH AS (SELECT $forTable.*, $KEY_EXCHANGE_RATE, $KEY_EQUIVALENT_AMOUNT, ${
    getAmountCalculation(
        homeCurrency.takeIf { forHome },
        forTable
    )
} AS $KEY_DISPLAY_AMOUNT FROM " + exchangeRateJoin(
    forTable,
    KEY_ACCOUNTID,
    homeCurrency
) + equivalentAmountJoin(homeCurrency) + ")"


fun buildTransactionGroupCte(
    accountQuery: String,
    selection: String?,
    forHome: String?,
    typeWithFallBack: String,
): String {
    // If a filter is applied to the transaction list, we need to calculate with the contents
    // of the archive otherwise we just take the archive itself into account
    val withFilter = selection != null
    val selection = listOfNotNull(accountQuery, selection?.takeIf { it.isNotEmpty() })
        .joinToString(" AND ")
    return buildString {
        append("WITH $CTE_SEARCH AS (SELECT $VIEW_WITH_ACCOUNT.* ")
        if(forHome != null) {
            append(",")
            append(KEY_EQUIVALENT_AMOUNT)
            append(",")
            append(KEY_EXCHANGE_RATE)
        }
        append(" FROM ")
        append(forHome?.let {
            exchangeRateJoin(
                VIEW_WITH_ACCOUNT,
                KEY_ACCOUNTID,
                it
            ) + equivalentAmountJoin(it)
        } ?: VIEW_WITH_ACCOUNT)
        append("), $CTE_TRANSACTION_GROUPS AS (SELECT ")
        append(KEY_DATE)
        append(",")
        append(KEY_TRANSFER_PEER)
        append(",")
        append("$typeWithFallBack AS $KEY_TYPE")
        append(",")
        append(" cast(CASE WHEN $KEY_CR_STATUS = '${CrStatus.VOID.name}' THEN 0 ELSE ")
        append(getAmountCalculation(forHome, CTE_SEARCH))
        append(" END AS integer) AS $KEY_DISPLAY_AMOUNT")
        append(" FROM $CTE_SEARCH ")
        append(" WHERE $WHERE_NOT_SPLIT AND ${if (withFilter) WHERE_NOT_ARCHIVE else WHERE_NOT_ARCHIVED} AND $selection")
        append(")")
    }
}

fun effectiveTypeExpression(typeWithFallback: String): String =
    "CASE $typeWithFallback WHEN $FLAG_NEUTRAL THEN CASE WHEN $KEY_AMOUNT > 0 THEN $FLAG_INCOME ELSE $FLAG_EXPENSE END ELSE $typeWithFallback END"

fun effectiveTypeExpressionIncludeTransfers(typeWithFallback: String): String =
    "CASE $typeWithFallback WHEN $FLAG_EXPENSE THEN $FLAG_EXPENSE WHEN $FLAG_INCOME THEN $FLAG_INCOME ELSE CASE WHEN $KEY_AMOUNT > 0 THEN $FLAG_INCOME ELSE $FLAG_EXPENSE END END"

fun transactionSumQuery(
    uri: Uri,
    projection: Array<String>,
    selectionIn: String?,
    typeWithFallBack: String,
    aggregateFunction: String,
    homeCurrency: String,
): String {

    val accountSelector = "$CTE_SEARCH.${uri.accountSelector}"
    val selection =
        if (TextUtils.isEmpty(selectionIn)) accountSelector else "$selectionIn AND $accountSelector"
    val aggregateNeutral = uri.getBooleanQueryParameter(QUERY_PARAMETER_AGGREGATE_NEUTRAL, false)
    val forHome =
        uri.getQueryParameter(KEY_ACCOUNTID) == null && uri.getQueryParameter(KEY_CURRENCY) == null

    return if (aggregateNeutral) {
        require(projection.size == 1)
        val column = projection.first()
        val type = when (column) {
            KEY_SUM_INCOME -> FLAG_INCOME
            KEY_SUM_EXPENSES -> FLAG_EXPENSE
            else -> throw IllegalArgumentException()
        }
        "WITH ${buildSearchCte(VIEW_WITH_ACCOUNT, homeCurrency, forHome)} SELECT $aggregateFunction($KEY_DISPLAY_AMOUNT) AS $column FROM $CTE_SEARCH WHERE $typeWithFallBack IN ($type, $FLAG_NEUTRAL) AND ($WHERE_NOT_SPLIT AND $WHERE_NOT_VOID AND $selection)"
    } else {
        val sumExpression = "$aggregateFunction($KEY_DISPLAY_AMOUNT)"
        val columns = projection.map {
            when (it) {
                KEY_SUM_EXPENSES -> "(SELECT $sumExpression FROM $CTE_TRANSACTION_AMOUNTS WHERE $KEY_TYPE = $FLAG_EXPENSE) AS $KEY_SUM_EXPENSES"
                KEY_SUM_INCOME -> "(SELECT $sumExpression FROM $CTE_TRANSACTION_AMOUNTS WHERE $KEY_TYPE = $FLAG_INCOME) AS $KEY_SUM_INCOME"
                else -> throw IllegalArgumentException()
            }
        }
        require(columns.isNotEmpty())
        val cteColumns = buildList {
            add("${effectiveTypeExpression(typeWithFallBack)} AS $KEY_TYPE")
            add(KEY_DISPLAY_AMOUNT)
        }
        """WITH ${buildSearchCte(VIEW_WITH_ACCOUNT, homeCurrency, forHome)},
            | $CTE_TRANSACTION_AMOUNTS AS (SELECT ${cteColumns.joinToString()} FROM $CTE_SEARCH WHERE ($WHERE_NOT_SPLIT AND $WHERE_NOT_VOID AND $selection))
            | SELECT ${columns.joinToString()}""".trimMargin()
    }
}

fun archiveSumCTE(
    archiveId: Long,
    typeWithFallBack: String,
): String {
    return buildString {
        append("WITH $CTE_TRANSACTION_AMOUNTS AS (SELECT ")
        append("$typeWithFallBack AS $KEY_TYPE")
        append(", cast(CASE WHEN $KEY_CR_STATUS = '${CrStatus.VOID.name}' THEN 0 ELSE ")
        append(KEY_AMOUNT)
        append(" END AS integer) AS $KEY_DISPLAY_AMOUNT")
        append(" FROM $VIEW_WITH_ACCOUNT")
        append(" WHERE $WHERE_NOT_SPLIT AND ($KEY_PARENTID = $archiveId OR (SELECT $KEY_PARENTID FROM $TABLE_TRANSACTIONS parents WHERE $KEY_ROWID = $VIEW_WITH_ACCOUNT.$KEY_PARENTID) = $archiveId))")
    }
}

//@formatter:off
fun getAmountHomeEquivalent(forTable: String, homeCurrency: String, currencyTable: String = forTable) =
    """cast(CASE WHEN $currencyTable.$KEY_CURRENCY = '$homeCurrency' THEN $KEY_AMOUNT ELSE round(coalesce(${calcEquivalentAmountForSplitParts(forTable)},coalesce($KEY_EXCHANGE_RATE,1) * $KEY_AMOUNT)) END AS integer)"""
//@formatter:on

fun calcEquivalentAmountForSplitParts(forTable: String) =
    "CASE WHEN $forTable.$KEY_PARENTID THEN (SELECT 1.0 * $KEY_EQUIVALENT_AMOUNT / $KEY_AMOUNT FROM $forTable parent WHERE $KEY_ROWID = $forTable.$KEY_PARENTID) * $KEY_AMOUNT ELSE $KEY_EQUIVALENT_AMOUNT END"

fun getExchangeRate(forTable: String, accountIdColumn: String, homeCurrency: String) = """
    coalesce((SELECT $KEY_EXCHANGE_RATE FROM $TABLE_ACCOUNT_EXCHANGE_RATES WHERE $KEY_ACCOUNTID = $forTable.$accountIdColumn AND $KEY_CURRENCY_SELF=$forTable.$KEY_CURRENCY AND $KEY_CURRENCY_OTHER='$homeCurrency'), 1)""".trimIndent()

fun getAmountCalculation(homeCurrency: String?, forTable: String, currencyTable: String = forTable) =
    if (homeCurrency != null) getAmountHomeEquivalent(forTable, homeCurrency, currencyTable) else KEY_AMOUNT

fun amountCteForDebts(homeCurrency: String, dateExpression: String?) =
    """$CTE_TRANSACTION_AMOUNTS AS (
    SELECT
    $KEY_ROWID,
    ${getAmountHomeEquivalent(VIEW_WITH_ACCOUNT, homeCurrency)} AS $KEY_EQUIVALENT_AMOUNT,
    $KEY_AMOUNT,
    $VIEW_WITH_ACCOUNT.$KEY_CURRENCY,
    $KEY_DEBT_ID
    FROM
    ${exchangeRateJoin(VIEW_WITH_ACCOUNT, KEY_ACCOUNTID, homeCurrency)}
    ${equivalentAmountJoin(homeCurrency)}
    ${dateExpression?.let { "WHERE $KEY_DATE <= $it" } ?: ""}
    )
    """.trimIndent()