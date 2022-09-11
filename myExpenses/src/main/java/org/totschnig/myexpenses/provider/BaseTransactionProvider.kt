package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.Bundle
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import timber.log.Timber
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

abstract class BaseTransactionProvider : ContentProvider() {
    var dirty = false
        set(value) {
            if (!field && value) {
                (context?.applicationContext as? MyApplication)?.markDataDirty()
            }
            field = value
        }

    lateinit var transactionDatabase: TransactionDatabase

    @Inject
    @Named(AppComponent.DATABASE_NAME)
    lateinit var databaseName: String

    @set:Inject
    var cursorFactory: SQLiteDatabase.CursorFactory? = null

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    private var shouldLog = false

    companion object {
        const val CURRENCIES_USAGES_TABLE_EXPRESSION =
            "$TABLE_CURRENCIES LEFT JOIN (SELECT coalesce($KEY_ORIGINAL_CURRENCY, $KEY_CURRENCY) AS currency_coalesced, count(*) AS $KEY_USAGES FROM $VIEW_EXTENDED GROUP BY currency_coalesced) on currency_coalesced = $KEY_CODE"

        val PAYEE_PROJECTION = arrayOf(
            KEY_ROWID,
            KEY_PAYEE_NAME,
            "exists (SELECT 1 FROM $TABLE_TRANSACTIONS WHERE $KEY_PAYEEID=$TABLE_PAYEES.$KEY_ROWID) AS $KEY_MAPPED_TRANSACTIONS",
            "exists (SELECT 1 FROM $TABLE_TEMPLATES WHERE $KEY_PAYEEID=$TABLE_PAYEES.$KEY_ROWID) AS $KEY_MAPPED_TEMPLATES",
            "(SELECT COUNT(*) FROM $TABLE_DEBTS WHERE $KEY_PAYEEID=$TABLE_PAYEES.$KEY_ROWID) AS $KEY_MAPPED_DEBTS"
        )
        const val DEBT_PAYEE_JOIN =
            "$TABLE_DEBTS LEFT JOIN $TABLE_PAYEES ON ($KEY_PAYEEID = $TABLE_PAYEES.$KEY_ROWID)"

        /**
         * @param transactionId When we edit a transaction, we want it to not be included into the debt sum, since it can be changed in the UI, and the variable amount will be calculated by the UI
         */
        fun debtProjection(transactionId: String?): Array<String> {
            val exclusionClause = transactionId?.let {
                "AND $KEY_ROWID != $it"
            } ?: ""
            return arrayOf(
                "$TABLE_DEBTS.$KEY_ROWID",
                KEY_PAYEEID,
                KEY_DATE,
                KEY_LABEL,
                KEY_AMOUNT,
                KEY_CURRENCY,
                KEY_DESCRIPTION,
                KEY_PAYEE_NAME,
                KEY_SEALED,
                "(select sum($KEY_AMOUNT) from $TABLE_TRANSACTIONS where $KEY_DEBT_ID = $TABLE_DEBTS.$KEY_ROWID $exclusionClause) AS $KEY_SUM"
            )
        }

        fun shortenComment(projectionIn: Array<String>): Array<String> = projectionIn.map {
            if (it == KEY_COMMENT)
                "case when instr($KEY_COMMENT, X'0A') > 0 THEN substr($KEY_COMMENT, 1, instr($KEY_COMMENT, X'0A')-1) else $KEY_COMMENT end AS $KEY_COMMENT"
            else
                it
        }.toTypedArray()

        const val KEY_DEBT_LABEL = "debt"

        const val DEBT_LABEL_EXPRESSION =
            "(SELECT $KEY_LABEL FROM $TABLE_DEBTS WHERE $KEY_ROWID = $KEY_DEBT_ID) AS $KEY_DEBT_LABEL"
        const val TAG = "TransactionProvider"
    }

    val homeCurrency: String
        get() = Utils.getHomeCurrency(context, prefHandler, userLocaleProvider)

    val accountsWithExchangeRate: String
        get() = exchangeRateJoin(TABLE_ACCOUNTS, KEY_ROWID, homeCurrency)

    val budgetTableJoin =
        "$TABLE_BUDGETS LEFT JOIN $TABLE_ACCOUNTS ON ($KEY_ACCOUNTID = $TABLE_ACCOUNTS.$KEY_ROWID)"

    private val fullAccountProjection =
        Account.PROJECTION_BASE.copyOf(Account.PROJECTION_BASE.size + 13).also {
            val baseLength = Account.PROJECTION_BASE.size
            it[baseLength] =
                "$KEY_OPENING_BALANCE + coalesce($KEY_CURRENT,0) AS $KEY_CURRENT_BALANCE"
            it[baseLength + 1] = KEY_SUM_INCOME
            it[baseLength + 2] = KEY_SUM_EXPENSES
            it[baseLength + 3] = KEY_SUM_TRANSFERS
            it[baseLength + 4] = "$KEY_OPENING_BALANCE + coalesce($KEY_TOTAL,0) AS $KEY_TOTAL"
            it[baseLength + 5] =
                "$KEY_OPENING_BALANCE + coalesce($KEY_CLEARED_TOTAL,0) AS $KEY_CLEARED_TOTAL"
            it[baseLength + 6] =
                "$KEY_OPENING_BALANCE + coalesce($KEY_RECONCILED_TOTAL,0) AS $KEY_RECONCILED_TOTAL"
            it[baseLength + 7] = KEY_USAGES
            it[baseLength + 8] =
                "0 AS $KEY_IS_AGGREGATE"//this is needed in the union with the aggregates to sort real accounts first
            it[baseLength + 9] = KEY_HAS_FUTURE
            it[baseLength + 10] = KEY_HAS_CLEARED
            it[baseLength + 11] = AccountType.sqlOrderExpression()
            it[baseLength + 12] = KEY_LAST_USED
        }

    fun buildAccountQuery(
        qb: SQLiteQueryBuilder,
        minimal: Boolean,
        mergeAggregate: String?,
        selection: String?,
        sortOrder: String?
    ): String {

        val aggregateFunction = TransactionProvider.aggregateFunction(
            prefHandler.getBoolean(
                PrefKey.DB_SAFE_MODE,
                false
            )
        )
        val cte = accountQueryCTE(
            homeCurrency,
            prefHandler.getString(PrefKey.CRITERION_FUTURE, "end_of_day") == "current",
            aggregateFunction
        )
        val joinWithAggregates =
            "$TABLE_ACCOUNTS LEFT JOIN aggregates ON $TABLE_ACCOUNTS.$KEY_ROWID = $KEY_ACCOUNTID"
        qb.tables = if (minimal) TABLE_ACCOUNTS else joinWithAggregates
        val query = if (mergeAggregate == null) {
            qb.buildQuery(
                fullAccountProjection, selection, null,
                null, null, null
            )
        } else {
            val subQueries: MutableList<String> = ArrayList()
            if (mergeAggregate == "1") {
                subQueries.add(
                    qb.buildQuery(
                        if (minimal) arrayOf(
                            KEY_ROWID,
                            KEY_LABEL,
                            KEY_CURRENCY,
                            "0 AS $KEY_IS_AGGREGATE"
                        ) else fullAccountProjection, selection, null,
                        null, null, null
                    )
                )
            }
            //Currency query
            if (mergeAggregate != Account.HOME_AGGREGATE_ID.toString()) {
                qb.tables =
                    "$joinWithAggregates LEFT JOIN $TABLE_CURRENCIES on $KEY_CODE = $KEY_CURRENCY"

                val rowIdColumn = "0 - $TABLE_CURRENCIES.$KEY_ROWID AS $KEY_ROWID"
                val labelColumn = "$KEY_CURRENCY AS $KEY_LABEL"
                val aggregateColumn = "1 AS $KEY_IS_AGGREGATE"
                val currencyProjection = if (minimal) arrayOf(
                    rowIdColumn,
                    labelColumn,
                    KEY_CURRENCY,
                    aggregateColumn
                ) else {
                    val openingBalanceSum = "$aggregateFunction($KEY_OPENING_BALANCE)"
                    arrayOf(
                        rowIdColumn,  //we use negative ids for aggregate accounts
                        labelColumn,
                        "'' AS $KEY_DESCRIPTION",
                        "$openingBalanceSum AS $KEY_OPENING_BALANCE",
                        KEY_CURRENCY,
                        "-1 AS $KEY_COLOR",
                        "$TABLE_CURRENCIES.$KEY_GROUPING",
                        "'AGGREGATE' AS $KEY_TYPE",
                        "0 AS $KEY_SORT_KEY",
                        "0 AS $KEY_EXCLUDE_FROM_TOTALS",
                        "null AS $KEY_SYNC_ACCOUNT_NAME",
                        "null AS $KEY_UUID",
                        "'DESC' AS $KEY_SORT_DIRECTION",
                        "1 AS $KEY_EXCHANGE_RATE",
                        "0 AS $KEY_CRITERION",
                        "0 AS $KEY_SEALED",
                        "$openingBalanceSum + coalesce($aggregateFunction($KEY_CURRENT),0) AS $KEY_CURRENT_BALANCE",
                        "$aggregateFunction($KEY_SUM_INCOME) AS $KEY_SUM_INCOME",
                        "$aggregateFunction($KEY_SUM_EXPENSES) AS $KEY_SUM_EXPENSES",
                        "$aggregateFunction($KEY_SUM_TRANSFERS) AS $KEY_SUM_TRANSFERS",
                        "$openingBalanceSum + coalesce($aggregateFunction($KEY_TOTAL),0) AS $KEY_TOTAL",
                        "0 AS $KEY_CLEARED_TOTAL",  //we do not calculate cleared and reconciled totals for aggregate accounts
                        "0 AS $KEY_RECONCILED_TOTAL",
                        "0 AS $KEY_USAGES",
                        aggregateColumn,
                        "max($KEY_HAS_FUTURE) AS $KEY_HAS_FUTURE",
                        "0 AS $KEY_HAS_CLEARED",
                        "0 AS $KEY_SORT_KEY_TYPE",
                        "0 AS $KEY_LAST_USED"
                    )
                }
                subQueries.add(
                    qb.buildQuery(
                        currencyProjection,
                        "$KEY_EXCLUDE_FROM_TOTALS = 0",
                        KEY_CURRENCY,
                        if (mergeAggregate == "1") "count(*) > 1" else "$TABLE_CURRENCIES.$KEY_ROWID = " +
                                mergeAggregate.substring(1),
                        null,
                        null
                    )
                )
            }
            //home query
            if (mergeAggregate == Account.HOME_AGGREGATE_ID.toString() || mergeAggregate == "1") {
                qb.tables = joinWithAggregates

                val grouping = prefHandler.getString(AggregateAccount.GROUPING_AGGREGATE, "NONE")
                val rowIdColumn = Account.HOME_AGGREGATE_ID.toString() + " AS " + KEY_ROWID
                val labelColumn = "'' AS $KEY_LABEL"
                val currencyColumn =
                    "'" + AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE + "' AS " + KEY_CURRENCY
                val aggregateColumn =
                    AggregateAccount.AGGREGATE_HOME.toString() + " AS " + KEY_IS_AGGREGATE
                val homeProjection = if (minimal) {
                    arrayOf(
                        rowIdColumn,
                        labelColumn,
                        currencyColumn,
                        aggregateColumn
                    )
                } else {
                    val openingBalanceSum =
                        "$aggregateFunction($KEY_OPENING_BALANCE * $KEY_EXCHANGE_RATE)"
                    arrayOf(
                        rowIdColumn,
                        labelColumn,
                        "'' AS $KEY_DESCRIPTION",
                        "$openingBalanceSum AS $KEY_OPENING_BALANCE",
                        currencyColumn,
                        "-1 AS $KEY_COLOR",
                        "'$grouping' AS $KEY_GROUPING",
                        "'AGGREGATE' AS $KEY_TYPE",
                        "0 AS $KEY_SORT_KEY",
                        "0 AS $KEY_EXCLUDE_FROM_TOTALS",
                        "null AS $KEY_SYNC_ACCOUNT_NAME",
                        "null AS $KEY_UUID",
                        "'DESC' AS $KEY_SORT_DIRECTION",
                        "1 AS $KEY_EXCHANGE_RATE",
                        "0 AS $KEY_CRITERION",
                        "0 AS $KEY_SEALED",
                        "$openingBalanceSum + coalesce($aggregateFunction(equivalent_current),0) AS $KEY_CURRENT_BALANCE",
                        "$aggregateFunction(equivalent_income) AS $KEY_SUM_INCOME",
                        "$aggregateFunction(equivalent_expense) AS $KEY_SUM_EXPENSES",
                        "0 AS $KEY_SUM_TRANSFERS",
                        "$openingBalanceSum + coalesce($aggregateFunction(equivalent_total),0) AS $KEY_TOTAL",
                        "0 AS $KEY_CLEARED_TOTAL",  //we do not calculate cleared and reconciled totals for aggregate accounts
                        "0 AS $KEY_RECONCILED_TOTAL",
                        "0 AS $KEY_USAGES",
                        aggregateColumn,
                        "max($KEY_HAS_FUTURE) AS $KEY_HAS_FUTURE",
                        "0 AS $KEY_HAS_CLEARED",
                        "0 AS $KEY_SORT_KEY_TYPE",
                        "0 AS $KEY_LAST_USED"
                    )
                }
                subQueries.add(
                    qb.buildQuery(
                        homeProjection,
                        "$KEY_EXCLUDE_FROM_TOTALS = 0",
                        "1",
                        "(select count(distinct $KEY_CURRENCY) from $TABLE_ACCOUNTS WHERE $KEY_CURRENCY != '$homeCurrency') > 0",
                        null,
                        null
                    )
                )
            }
            val grouping = if (!minimal) {
                when (try {
                    AccountGrouping.valueOf(
                        prefHandler.getString(
                            PrefKey.ACCOUNT_GROUPING, AccountGrouping.TYPE.name
                        )!!
                    )
                } catch (e: IllegalArgumentException) {
                    AccountGrouping.TYPE
                }) {
                    AccountGrouping.CURRENCY -> "$KEY_CURRENCY,$KEY_IS_AGGREGATE"
                    AccountGrouping.TYPE -> "$KEY_IS_AGGREGATE,$KEY_SORT_KEY_TYPE"
                    else -> KEY_IS_AGGREGATE
                }
            } else KEY_IS_AGGREGATE
            qb.buildUnionQuery(
                subQueries.toTypedArray(),
                "$grouping,$sortOrder",
                null
            )
        }
        return "$cte\n$query"
    }

    fun backup(context: Context, backupDir: File): Result<Unit> {
        val currentDb = File(transactionDatabase.readableDatabase.path)
        transactionDatabase.readableDatabase.beginTransaction()
        return try {
            backupDb(getBackupDbFile(backupDir), currentDb).mapCatching {
                val backupPrefFile = getBackupPrefFile(backupDir)
                // Samsung has special path on some devices
                // http://stackoverflow.com/questions/5531289/copy-the-shared-preferences-xml-file-from-data-on-samsung-device-failed
                val sharedPrefPath = "/shared_prefs/" + context.packageName + "_preferences.xml"
                var sharedPrefFile =
                    File("/dbdata/databases/" + context.packageName + sharedPrefPath)
                if (!sharedPrefFile.exists()) {
                    sharedPrefFile = File(getInternalAppDir().path + sharedPrefPath)
                    log(sharedPrefFile.path)
                    if (!sharedPrefFile.exists()) {
                        val message = "Unable to find shared preference file at " +
                                sharedPrefFile.path
                        report(message)
                        throw Throwable(message)
                    }
                }
                dirty = if (FileCopyUtils.copy(sharedPrefFile, backupPrefFile)) {
                    prefHandler.putBoolean(PrefKey.AUTO_BACKUP_DIRTY, false)
                    false
                } else {
                    val message = "Unable to copy preference file from  " +
                            sharedPrefFile.path + " to " + backupPrefFile.path
                    throw Throwable(message)
                }
            }
        } finally {
            transactionDatabase.readableDatabase.endTransaction()
        }
    }

    fun budgetCategoryUpsert(db: SQLiteDatabase, uri: Uri, values: ContentValues): Int {
        val (budgetId, catId) = parseBudgetCategoryUri(uri)
        val year: String? = values.getAsString(KEY_YEAR)
        val second: String? = values.getAsString(KEY_SECOND_GROUP)
        val budget: String? = values.getAsString(KEY_BUDGET)
        val oneTime: String? = values.getAsBoolean(KEY_ONE_TIME)?.let { if (it) "1" else "0" }
        val rollOverPrevious: String? = values.getAsString(KEY_BUDGET_ROLLOVER_PREVIOUS)
        val rollOverNext: String? = values.getAsString(KEY_BUDGET_ROLLOVER_NEXT)
        check(
            (budget != null && rollOverNext == null && rollOverPrevious == null) ||
                    (budget == null && oneTime == null && year != null &&
                            (rollOverNext != null).xor(rollOverPrevious != null)
                            )
        )
        val statementBuilder = StringBuilder()
        statementBuilder.append("INSERT OR REPLACE INTO $TABLE_BUDGET_ALLOCATIONS ($KEY_BUDGETID, $KEY_CATID, $KEY_YEAR, $KEY_SECOND_GROUP, $KEY_BUDGET_ROLLOVER_PREVIOUS, $KEY_BUDGET_ROLLOVER_NEXT, $KEY_BUDGET, $KEY_ONE_TIME) ")
        statementBuilder.append("VALUES (?,?,?,?,")
        val baseArgs = listOf(budgetId, catId, year, second)
        val argsList = mutableListOf<String?>()
        argsList.addAll(baseArgs)
        if (rollOverPrevious == null) {
            statementBuilder.append("(select $KEY_BUDGET_ROLLOVER_PREVIOUS from $TABLE_BUDGET_ALLOCATIONS where $KEY_BUDGETID = ? AND $KEY_CATID = ? AND $KEY_YEAR = ? AND $KEY_SECOND_GROUP = ?),")
            argsList.addAll(baseArgs)
        } else {
            statementBuilder.append("?,")
            argsList.add(rollOverPrevious)
        }
        if (rollOverNext == null) {
            statementBuilder.append("(select $KEY_BUDGET_ROLLOVER_NEXT from $TABLE_BUDGET_ALLOCATIONS where $KEY_BUDGETID = ? AND $KEY_CATID = ? AND $KEY_YEAR = ? AND $KEY_SECOND_GROUP = ?),")
            argsList.addAll(baseArgs)
        } else {
            statementBuilder.append("?,")
            argsList.add(rollOverNext)
        }
        if (budget != null) {
            statementBuilder.append("?,?)")
            argsList.add(budget)
            argsList.add(oneTime ?: "1")
        } else {
            statementBuilder.append("(select $KEY_BUDGET from $TABLE_BUDGET_ALLOCATIONS where $KEY_BUDGETID = ? AND $KEY_CATID = ? AND $KEY_YEAR = ? AND $KEY_SECOND_GROUP = ?),")
            statementBuilder.append("(select $KEY_ONE_TIME from $TABLE_BUDGET_ALLOCATIONS where $KEY_BUDGETID = ? AND $KEY_CATID = ? AND $KEY_YEAR = ? AND $KEY_SECOND_GROUP = ?))")
            argsList.addAll(baseArgs)
            argsList.addAll(baseArgs)
        }
        val statement = db.compileStatement(statementBuilder.toString())
        argsList.forEachIndexed { index, arg ->
            val bindIndex = index + 1
            if (arg != null) {
                statement.bindString(bindIndex, arg)
            } else {
                statement.bindNull(bindIndex)
            }
        }
        log("$statement - ${argsList.joinToString()}")
        return statement.executeUpdateDelete()
    }

    /**
     * @return number of corrupted entries
     */
    fun checkCorruptedData987() = Bundle(1).apply {
        putLongArray(KEY_RESULT, transactionDatabase.readableDatabase.rawQuery(
            "select distinct transactions.parent_id from transactions left join transactions parent on transactions.parent_id = parent._id where transactions.parent_id is not null and parent.account_id != transactions.account_id",
            null
        ).use { cursor ->
            cursor.asSequence.map { it.getLong(0) }.toList().toLongArray()
        })
    }

    private fun backupDb(backupDb: File, currentDb: File): Result<Unit> {
        if (currentDb.exists()) {
            if (FileCopyUtils.copy(currentDb, backupDb)) {
                return ResultUnit
            }
            return Result.failure(Throwable("Error while copying ${currentDb.path} to ${backupDb.path}"))
        }
        return Result.failure(Throwable("Could not find database at ${currentDb.path}"))
    }

    fun initOpenHelper() {
        transactionDatabase = TransactionDatabase(context, databaseName, cursorFactory)
    }

    fun getInternalAppDir(): File {
        return context!!.filesDir.parentFile!!
    }

    fun log(message: String, vararg args: Any) {
        Timber.tag(TAG).i(message, *args)
    }

    fun SQLiteQueryBuilder.measureAndLogQuery(
        uri: Uri,
        db: SQLiteDatabase,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        groupBy: String?,
        having: String?,
        sortOrder: String?,
        limit: String?
    ): Cursor = measure(block = {
        query(
            db,
            projection,
            selection,
            selectionArgs,
            groupBy,
            having,
            sortOrder,
            limit
        )
    }) {
        "$uri - ${
            buildQuery(
                projection,
                selection,
                groupBy,
                having,
                sortOrder,
                limit
            )
        } - (${selectionArgs?.joinToString()})"
    }

    fun SQLiteDatabase.measureAndLogQuery(
        uri: Uri,
        selection: String?,
        sql: String,
        selectionArgs: Array<String>?
    ): Cursor = measure(block = { rawQuery(sql, selectionArgs) }) {
        "$uri - $selection - $sql - (${selectionArgs?.joinToString()})"
    }

    private fun <T : Any> measure(block: () -> T, lazyMessage: () -> String): T {
        if (shouldLog) {
            val startTime = Instant.now()
            val result = block()
            val endTime = Instant.now()
            val duration = Duration.between(startTime, endTime)
            log("${lazyMessage()} : $duration")
            return result
        }
        return block()
    }

    fun report(e: String) {
        CrashHandler.report(Exception(e), TAG)
    }

    override fun onCreate(): Boolean {
        MyApplication.getInstance().appComponent.inject(this)
        shouldLog = prefHandler.getBoolean(PrefKey.DEBUG_LOGGING, BuildConfig.DEBUG)
        initOpenHelper()
        return true
    }
}