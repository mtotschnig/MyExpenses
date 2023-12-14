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
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_AGGREGATE_NEUTRAL
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ALLOCATED_ONLY
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_TRANSACTION_ID_LIST
import org.totschnig.myexpenses.provider.filter.WhereFilter

private fun requireIdParameter(parameter: String) {
    require(parameter.isDigitsOnly())
}

/**
 * - with parameter QUERY_PARAMETER_TRANSACTION_ID_LIST return requested ids
 * - with parameter KEY_TRANSACTIONID return the transaction and its split parts
 *   (used from Transaction Detail Fragment)
 * - with parameter KEY_PARENTID return just the split parts
 *   (used from Edit split)
 * - otherwise accountSelector logic is applied
 */
val Uri.transactionQuerySelector: String
    get() = getQueryParameter(QUERY_PARAMETER_TRANSACTION_ID_LIST)?.let { idList ->
        idList.split(',').forEach { requireIdParameter(it.trim()) }
        "$KEY_ROWID IN ($idList)"
    } ?:  getQueryParameter(KEY_TRANSACTIONID)?.let {
        requireIdParameter(it)
        "($KEY_ROWID = $it OR $KEY_PARENTID = $it)"
    } ?:  getQueryParameter(KEY_PARENTID)?.let {
        requireIdParameter(it)
        "$KEY_PARENTID = $it"
    } ?: accountSelector


val Uri.templateQuerySelector: String?
    get() = getQueryParameter(KEY_PARENTID)?.let {
        requireIdParameter(it)
        "$KEY_PARENTID = $it"
    }

/**
 * with paramter KEY_ACCOUNTID show single account, with parameter KEY_CURRENCY show for all
 * accounts with given currency (if not excluded from totals), otherwise all transactions (if not
 * excluded from totals) = Grand total account
 */

val Uri.accountSelector: String
    get() =
        KEY_ACCOUNTID + (
                getQueryParameter(KEY_ACCOUNTID)?.let {
                    requireIdParameter(it)
                    "= $it"
                }
                    ?: (" IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_EXCLUDE_FROM_TOTALS=0 " +
                            (getQueryParameter(KEY_CURRENCY)?.let {
                                require(it.matches("[A-Z]{3}".toRegex()))
                                "AND $KEY_CURRENCY = '$it'"
                            } ?: "") +
                            ")"
                            )
                )

fun Uri.amountCalculation(tableName: String, homeCurrency: String, withAlias: Boolean = false): String =
    if (getQueryParameter(KEY_ACCOUNTID) != null || getQueryParameter(KEY_CURRENCY) != null)
        KEY_AMOUNT else getAmountHomeEquivalent(tableName, homeCurrency) +
            if(withAlias) " AS $KEY_AMOUNT" else ""

fun checkSealedWithAlias(baseTable: String, innerTable: String) =
    "max(" + checkForSealedAccount(
        baseTable,
        innerTable
    ) + ", " + checkForSealedDebt(baseTable) + ") AS " + KEY_SEALED

/**
 * we check if the object is linked to a sealed account, either via its account, it transfer_account, or its children.
 * For Children, we only need to check for transfer_account, since there account is identical to their parent.
 */
fun checkForSealedAccount(baseTable: String, innerTable: String, withTransfer: Boolean = true) =
    if (withTransfer)
        "(SELECT max($KEY_SEALED) FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = $KEY_ACCOUNTID OR $KEY_ROWID = $KEY_TRANSFER_ACCOUNT OR $KEY_ROWID in (SELECT $KEY_TRANSFER_ACCOUNT FROM $innerTable WHERE $KEY_PARENTID = $baseTable.$KEY_ROWID))"
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
    typeParameter: String? = null
) = categoryTreeCTE(
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

fun budgetColumn(year: String?, second: String?): String {
    val mainSelect = subSelectFromAllocations(
        KEY_BUDGET,
        year,
        second,
        false
    )
    return (if (year == null) mainSelect else "coalesce($mainSelect," +
            "(SELECT $KEY_BUDGET from Allocations WHERE $KEY_ONE_TIME = 0 AND (coalesce($KEY_YEAR,0) < $year ${second?.let { " OR (coalesce($KEY_YEAR,0) = $year AND coalesce($KEY_SECOND_GROUP,0) < $it)" } ?: ""}) ORDER BY $KEY_YEAR DESC ${if (second == null) "" else ", $KEY_SECOND_GROUP DESC"} LIMIT 1))") +
            " AS $KEY_BUDGET"
}

fun subSelectFromAllocations(
    column: String,
    year: String?,
    second: String?,
    withAlias: Boolean = true
) =
    "(SELECT $column from Allocations ${budgetSelectForGroup(year, second)})" +
            if (withAlias) " AS $column" else ""

fun categoryTreeWithSum(
    aggregateFunction: String,
    homeCurrency: String,
    sortOrder: String? = null,
    selection: String? = null,
    projection: Array<String>,
    uri: Uri
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
                val amountStatement = if (aggregateNeutral) KEY_AMOUNT else
                    //the ELSE in the CASE statement is FLAG_NEUTRAL because the categoryTreeCTE
                    // returns categories which are either the requested type or neutral
                    "CASE $KEY_TYPE WHEN $type THEN $KEY_AMOUNT ELSE ${if (incomeType) "max" else "min"}($KEY_AMOUNT, 0) END"
                append("(SELECT $aggregateFunction($amountStatement) FROM amounts ")
                append(") AS $KEY_SUM")
            }

            KEY_BUDGET -> budgetColumn(year, second)
            KEY_BUDGET_ROLLOVER_NEXT, KEY_BUDGET_ROLLOVER_PREVIOUS, KEY_ONE_TIME ->
                subSelectFromAllocations(it, year, second)

            else -> it
        }
    }
    return buildString {
        append(
            categoryTreeCTE(
                sortOrder = sortOrder,
                type = type
            )
        )
        val amountCalculation = uri.amountCalculation(VIEW_WITH_ACCOUNT, homeCurrency, true)
        append(", amounts as (select $amountCalculation from $VIEW_WITH_ACCOUNT WHERE ")
        append(WHERE_NOT_VOID)
        append(" AND +$accountSelector")
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
        if (uri.getBooleanQueryParameter(QUERY_PARAMETER_ALLOCATED_ONLY, false)) {
            append(" WHERE $KEY_BUDGET IS NOT NULL OR $KEY_SUM IS NOT NULL")
        }
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

fun budgetAllocation(uri: Uri): String {
    val year = uri.getQueryParameter(KEY_YEAR)
    val second = uri.getQueryParameter(KEY_SECOND_GROUP)
    val cte = budgetAllocationsCTE(budgetSelect(uri))
    return "WITH $cte SELECT " +
            budgetColumn(year, second) + "," +
            subSelectFromAllocations(KEY_BUDGET_ROLLOVER_PREVIOUS, year, second) + "," +
            subSelectFromAllocations(KEY_BUDGET_ROLLOVER_NEXT, year, second) + "," +
            subSelectFromAllocations(KEY_ONE_TIME, year, second)
}

fun categoryTreeWithMappedObjects(
    selection: String,
    projection: Array<String>,
    aggregate: Boolean
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
    return """
            ${categoryTreeCTE(rootExpression = "$KEY_ROWID = $TABLE_CATEGORIES.$KEY_ROWID")}
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

@JvmOverloads
fun getCategoryTreeForView(
    rootExpression: String = "$KEY_PARENTID IS NULL",
    withRootLabel: Boolean = true
): String {
    val rootPath = if (withRootLabel) "main.$KEY_LABEL" else "''"
    val separator = if (withRootLabel) "' > '" else
        "CASE WHEN Tree.$KEY_PATH = '' THEN '' ELSE ' > ' END"
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
    WHERE $KEY_PARENTID IS NULL ${selection?.let { " AND $it" } ?: ""}
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
    type: Byte? = null
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
    return """
WITH Tree AS (
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
    futureStartsNow: Boolean,
    aggregateFunction: String,
    typeWithFallBack: String
): String {
    val futureCriterion =
        if (futureStartsNow) "'now'" else "'now', 'localtime', 'start of day', '+1 day', 'utc'"
    val isExpense =
        "$KEY_TYPE = $FLAG_EXPENSE OR ($KEY_TYPE = $FLAG_NEUTRAL AND $KEY_AMOUNT < 0)"
    val isIncome =
        "$KEY_TYPE = $FLAG_INCOME OR ($KEY_TYPE = $FLAG_NEUTRAL AND $KEY_AMOUNT > 0)"
    val isTransfer = "$KEY_TYPE = $FLAG_TRANSFER"
    return """
WITH now as (
    SELECT
        cast(strftime('%s', $futureCriterion) as integer) AS now
), amounts AS (
    SELECT
        $KEY_AMOUNT,
        $typeWithFallBack AS $KEY_TYPE,
        $KEY_TRANSFER_PEER,
        $KEY_CR_STATUS,
        $KEY_DATE,
        coalesce(
            CASE
                WHEN $KEY_PARENTID
                THEN (SELECT 1.0 * $KEY_EQUIVALENT_AMOUNT / $KEY_AMOUNT FROM $TABLE_TRANSACTIONS
                    WHERE $KEY_ROWID = $VIEW_WITH_ACCOUNT.$KEY_PARENTID
                  ) * $KEY_AMOUNT
                ELSE $KEY_EQUIVALENT_AMOUNT
            END,
            coalesce($KEY_EXCHANGE_RATE, 1) * amount
        ) AS $KEY_EQUIVALENT_AMOUNT,
        $VIEW_WITH_ACCOUNT.$KEY_ACCOUNTID 
    FROM ${exchangeRateJoin(VIEW_WITH_ACCOUNT, KEY_ACCOUNTID, homeCurrency)}
    WHERE $WHERE_NOT_SPLIT AND $KEY_CR_STATUS != '${CrStatus.VOID.name}'
), aggregates AS (
    SELECT
        $KEY_ACCOUNTID,
        $aggregateFunction($KEY_AMOUNT) as $KEY_TOTAL,
        $aggregateFunction($KEY_EQUIVALENT_AMOUNT) as equivalent_total,
        $aggregateFunction(CASE WHEN $isIncome THEN $KEY_AMOUNT ELSE 0 END) as $KEY_SUM_INCOME,
        $aggregateFunction(CASE WHEN $isIncome THEN $KEY_EQUIVALENT_AMOUNT ELSE 0 END) as equivalent_income,
        $aggregateFunction(CASE WHEN $isExpense THEN $KEY_AMOUNT ELSE 0 END) as $KEY_SUM_EXPENSES,
        $aggregateFunction(CASE WHEN $isExpense THEN $KEY_EQUIVALENT_AMOUNT ELSE 0 END) as equivalent_expense,
        $aggregateFunction(CASE WHEN $isTransfer THEN $KEY_AMOUNT ELSE 0  END) as $KEY_SUM_TRANSFERS,
        $aggregateFunction(CASE WHEN $KEY_DATE < (select now from now) THEN $KEY_AMOUNT ELSE 0 END) as $KEY_CURRENT,
        $aggregateFunction(CASE WHEN $KEY_DATE < (select now from now) THEN $KEY_EQUIVALENT_AMOUNT ELSE 0 END) as equivalent_current,
        $aggregateFunction(CASE WHEN $KEY_CR_STATUS IN ( 'RECONCILED', 'CLEARED' ) THEN $KEY_AMOUNT ELSE 0 END) as $KEY_CLEARED_TOTAL,
        $aggregateFunction(CASE WHEN $KEY_CR_STATUS = 'RECONCILED' THEN $KEY_AMOUNT ELSE 0 END) as $KEY_RECONCILED_TOTAL,
        max(CASE WHEN $KEY_CR_STATUS = 'CLEARED' THEN 1 ELSE 0 END) as $KEY_HAS_CLEARED,
        max($KEY_DATE) >= (select now from now) as $KEY_HAS_FUTURE
   from amounts group by $KEY_ACCOUNTID
)
"""
}

fun exchangeRateJoin(
    table: String,
    colum: String,
    homeCurrency: String,
    joinTable: String = table
) = """
    $table LEFT JOIN $TABLE_ACCOUNT_EXCHANGE_RATES
        ON $joinTable.$colum = $TABLE_ACCOUNT_EXCHANGE_RATES.$KEY_ACCOUNTID
        AND $KEY_CURRENCY_SELF = $joinTable.$KEY_CURRENCY
        AND $KEY_CURRENCY_OTHER = '$homeCurrency'
""".trimIndent()

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

const val TAG_LIST_EXPRESSION = "group_concat($TABLE_TAGS.$KEY_LABEL,'') AS $KEY_TAGLIST"

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
    associateColumn: String
) =
    " LEFT JOIN $joinTable ON $joinTable.$mainColumn = $mainTable.$KEY_ROWID LEFT JOIN $associatedTable ON $associateColumn= $associatedTable.$KEY_ROWID"

fun tagGroupBy(tableName: String): String =
    " GROUP BY $tableName.$KEY_ROWID"

fun buildTransactionRowSelect(filter: WhereFilter?) =
    "SELECT $KEY_ROWID from $TABLE_TRANSACTIONS WHERE $KEY_ACCOUNTID = ?" +
            if (filter?.isEmpty == false) {
                " AND ${filter.getSelectionForParents(TABLE_TRANSACTIONS, true)}"
            } else ""

fun transactionListAsCTE(catId: String) =
    getCategoryTreeForView("$KEY_ROWID = $catId", false) +
            ", $VIEW_COMMITTED AS (" +
            transactionsJoin() +
            " WHERE $KEY_STATUS != $STATUS_UNCOMMITTED " +
            tagGroupBy(TABLE_TRANSACTIONS) +
            ")"

fun buildViewDefinition(tableName: String) =
    " AS ${getCategoryTreeForView()} ${transactionsJoin(tableName, false)}"

private fun transactionsJoin(
    tableName: String = TABLE_TRANSACTIONS,
    withPlanInstance: Boolean = tableName == TABLE_TRANSACTIONS
) = buildString {
    append(" SELECT $tableName.*, Tree.$KEY_PATH, Tree.$KEY_ICON, Tree.$KEY_TYPE,  $TABLE_PAYEES.$KEY_PAYEE_NAME, $TABLE_METHODS.$KEY_LABEL AS $KEY_METHOD_LABEL, $TABLE_METHODS.$KEY_ICON AS $KEY_METHOD_ICON")
    if (withPlanInstance) {
        append(", $TABLE_PLAN_INSTANCE_STATUS.$KEY_TEMPLATEID")
    }
    append(", $TAG_LIST_EXPRESSION")
    append(", $KEY_CURRENCY")
    append(
        """ FROM $tableName
        | LEFT JOIN $TABLE_PAYEES ON $KEY_PAYEEID = $TABLE_PAYEES.$KEY_ROWID
        | LEFT JOIN $TABLE_METHODS ON $KEY_METHODID = $TABLE_METHODS.$KEY_ROWID
        | LEFT JOIN $TABLE_ACCOUNTS ON $KEY_ACCOUNTID = $TABLE_ACCOUNTS.$KEY_ROWID
        | LEFT JOIN Tree ON $KEY_CATID = TREE.$KEY_ROWID""".trimMargin()
    )
    if (withPlanInstance) {
        append(" LEFT JOIN $TABLE_PLAN_INSTANCE_STATUS ON $tableName.$KEY_ROWID = $TABLE_PLAN_INSTANCE_STATUS.$KEY_TRANSACTIONID")
    }
    append(tagJoin(tableName))
}

const val CTE_TRANSACTION_GROUPS = "cte_transaction_groups"
const val CTE_TRANSACTION_AMOUNTS = "cte_amounts"

fun buildTransactionGroupCte(
    selection: String,
    forHome: String?,
    typeWithFallBack: String
): String {
    return buildString {
        append("WITH $CTE_TRANSACTION_GROUPS AS (SELECT ")
        append(KEY_DATE)
        append(",")
        append(KEY_TRANSFER_PEER)
        append(",")
        append("$typeWithFallBack AS $KEY_TYPE")
        append(", cast(")
        append(getAmountCalculation(forHome))
        append(" AS integer) AS $KEY_DISPLAY_AMOUNT")
        append(" FROM $VIEW_WITH_ACCOUNT")
        append(" WHERE $WHERE_NOT_SPLIT AND $WHERE_NOT_VOID AND $selection)")
    }
}

fun effectiveTypeExpression(typeWithFallback: String): String =
    "CASE $typeWithFallback WHEN $FLAG_NEUTRAL THEN CASE WHEN $KEY_AMOUNT > 0 THEN $FLAG_INCOME ELSE $FLAG_EXPENSE END ELSE $typeWithFallback END"

fun transactionSumQuery(
    uri: Uri,
    projection: Array<String>,
    selectionIn: String?,
    typeWithFallBack: String,
    aggregateFunction: String,
    homeCurrency: String
): String {
    val accountSelector: String = uri.accountSelector
    val selection =
        if (TextUtils.isEmpty(selectionIn)) accountSelector else "$selectionIn AND $accountSelector"
    val aggregateNeutral = uri.getBooleanQueryParameter(QUERY_PARAMETER_AGGREGATE_NEUTRAL, false)

    return if (aggregateNeutral) {
        require(projection.size == 1)
        val column = projection.first()
        val type = when (column) {
            KEY_SUM_INCOME -> FLAG_INCOME
            KEY_SUM_EXPENSES -> FLAG_EXPENSE
            else -> throw IllegalArgumentException()
        }
        val sumExpression =
            "$aggregateFunction(${uri.amountCalculation(VIEW_WITH_ACCOUNT, homeCurrency, false)})"
        """SELECT $sumExpression AS $column FROM $VIEW_WITH_ACCOUNT WHERE $typeWithFallBack IN ($type, $FLAG_NEUTRAL)
AND ($KEY_CATID IS NOT $SPLIT_CATID AND $KEY_CR_STATUS != 'VOID' AND $selection)"""
    } else {
        val sumExpression =
            "$aggregateFunction(${uri.amountCalculation(CTE_TRANSACTION_AMOUNTS, homeCurrency, false)})"
        val columns = projection.map {
            when (it) {
                KEY_SUM_EXPENSES -> "(SELECT $sumExpression FROM $CTE_TRANSACTION_AMOUNTS WHERE $KEY_TYPE = $FLAG_EXPENSE) AS $KEY_SUM_EXPENSES"
                KEY_SUM_INCOME -> "(SELECT $sumExpression FROM $CTE_TRANSACTION_AMOUNTS WHERE $KEY_TYPE = $FLAG_INCOME) AS $KEY_SUM_INCOME"
                else -> throw IllegalArgumentException()
            }
        }
        require(columns.isNotEmpty())
        """WITH $CTE_TRANSACTION_AMOUNTS AS (
    SELECT ${effectiveTypeExpression(typeWithFallBack)} AS $KEY_TYPE, $KEY_AMOUNT, $KEY_PARENTID, $KEY_ACCOUNTID, $KEY_CURRENCY, $KEY_EQUIVALENT_AMOUNT FROM $VIEW_WITH_ACCOUNT
    WHERE ($KEY_CATID IS NOT $SPLIT_CATID AND $KEY_CR_STATUS != 'VOID' AND $selection))
    SELECT ${columns.joinToString()}"""
    }
}