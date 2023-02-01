package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.FutureCriterion
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.DataModule
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_CALLER_IS_IN_BULK
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import timber.log.Timber
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.math.abs
import kotlin.math.pow

fun Uri.Builder.appendBooleanQueryParameter(key: String): Uri.Builder =
    appendQueryParameter(key, "1")

abstract class BaseTransactionProvider : ContentProvider() {
    var dirty = false
        set(value) {
            if (!field && value) {
                (context?.applicationContext as? MyApplication)?.markDataDirty()
            }
            field = value
        }

    fun maybeSetDirty(uriMatch: Int) {
        if (uriMatch != EVENT_CACHE) {
            dirty = true
        }
    }

    var _helper: SupportSQLiteOpenHelper? = null

    val helper: SupportSQLiteOpenHelper
        get() = requireHelper()

    @Inject
    @Named(AppComponent.DATABASE_NAME)
    @JvmSuppressWildcards
    lateinit var provideDatabaseName: (Boolean) -> String

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var openHelperProvider: Provider<SupportSQLiteOpenHelper>

    val collate: String
        get() = prefHandler.collate

    val wrappedContext: Context
        get() = userLocaleProvider.wrapContext(context!!)

    private var shouldLog = false

    var bulkInProgress = false

    private val bulkNotificationUris = mutableSetOf<Pair<Uri, Boolean>>()

    fun notifyAccountChange() {
        notifyChange(TransactionProvider.ACCOUNTS_BASE_URI, false)
        notifyChange(TransactionProvider.ACCOUNTS_MINIMAL_URI, false)
    }

    fun notifyChange(uri: Uri, syncToNetwork: Boolean) {
        if (!bulkInProgress && callerIsNotInBulkOperation(uri)) {
            notifyChangeDo(uri, syncToNetwork)
        } else {
            synchronized(bulkNotificationUris) {
                bulkNotificationUris.add(uri to syncToNetwork)
            }
        }
    }

    private fun notifyChangeDo(uri: Uri, syncToNetwork: Boolean) {
        context!!.contentResolver.notifyChange(
            uri, null,
            syncToNetwork && prefHandler.getBoolean(PrefKey.SYNC_CHANGES_IMMEDIATELY, true)
        )
    }

    fun notifyBulk() {
        synchronized(bulkNotificationUris) {
            val iterator = bulkNotificationUris.iterator()
            for ((uri, syncToNetwork) in iterator) {
                notifyChangeDo(uri, syncToNetwork)
                iterator.remove()
            }
        }
    }

    fun callerIsNotSyncAdapter(uri: Uri): Boolean {
        return !uri.getBooleanQueryParameter(
            TransactionProvider.QUERY_PARAMETER_CALLER_IS_SYNCADAPTER,
            false
        )
    }

    fun callerIsNotInBulkOperation(uri: Uri): Boolean {
        return !uri.getBooleanQueryParameter(QUERY_PARAMETER_CALLER_IS_IN_BULK, false)
    }

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

        fun defaultBudgetAllocationUri(accountId: Long, grouping: Grouping): Uri =
            TransactionProvider.BUDGETS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_DEFAULT_BUDGET_ALLOCATIONS)
                .appendPath(accountId.toString())
                .appendPath(grouping.name)
                .build()

        protected const val TRANSACTIONS = 1
        protected const val TRANSACTION_ID = 2
        protected const val CATEGORIES = 3
        protected const val ACCOUNTS = 4
        protected const val ACCOUNTS_BASE = 5
        protected const val ACCOUNT_ID = 6
        protected const val PAYEES = 7
        protected const val METHODS = 8
        protected const val METHOD_ID = 9
        protected const val ACCOUNTTYPES_METHODS = 10
        protected const val TEMPLATES = 11
        protected const val TEMPLATE_ID = 12
        protected const val CATEGORY_ID = 13
        protected const val PAYEE_ID = 15
        protected const val METHODS_FILTERED = 16
        protected const val TEMPLATES_INCREASE_USAGE = 17
        protected const val SQLITE_SEQUENCE_TABLE = 19
        protected const val AGGREGATE_ID = 20
        protected const val UNCOMMITTED = 21
        protected const val TRANSACTIONS_GROUPS = 22
        protected const val TRANSACTIONS_SUMS = 24
        protected const val TRANSACTION_MOVE = 25
        protected const val PLANINSTANCE_TRANSACTION_STATUS = 26
        protected const val CURRENCIES = 27
        protected const val TRANSACTION_TOGGLE_CRSTATUS = 29
        protected const val MAPPED_METHODS = 31
        protected const val DUAL = 32
        protected const val CURRENCIES_CHANGE_FRACTION_DIGITS = 33
        protected const val EVENT_CACHE = 34
        protected const val DEBUG_SCHEMA = 35
        protected const val STALE_IMAGES = 36
        protected const val STALE_IMAGES_ID = 37
        protected const val TRANSACTION_UNDELETE = 38
        protected const val TRANSACTIONS_LASTEXCHANGE = 39
        protected const val ACCOUNTS_SWAP_SORT_KEY = 40
        protected const val MAPPED_TRANSFER_ACCOUNTS = 41
        protected const val CHANGES = 42
        protected const val SETTINGS = 43
        protected const val TEMPLATES_UNCOMMITTED = 44
        protected const val ACCOUNT_ID_GROUPING = 45
        protected const val ACCOUNT_ID_SORT_DIRECTION = 46
        protected const val AUTOFILL = 47
        protected const val ACCOUNT_EXCHANGE_RATE = 48
        protected const val UNSPLIT = 49
        protected const val BUDGETS = 50
        protected const val BUDGET_ID = 51
        protected const val BUDGET_CATEGORY = 52
        protected const val CURRENCIES_CODE = 53
        protected const val ACCOUNTS_MINIMAL = 54
        protected const val TAGS = 55
        protected const val TRANSACTIONS_TAGS = 56
        protected const val TAG_ID = 57
        protected const val TEMPLATES_TAGS = 58
        protected const val UNCOMMITTED_ID = 59
        protected const val PLANINSTANCE_STATUS_SINGLE = 60
        protected const val TRANSACTION_LINK_TRANSFER = 61
        protected const val ACCOUNTS_TAGS = 62
        protected const val DEBTS = 63
        protected const val DEBT_ID = 64
        protected const val BUDGET_ALLOCATIONS = 65
        protected const val ACCOUNT_DEFAULT_BUDGET_ALLOCATIONS = 66
    }

    val homeCurrency: String
        get() = Utils.getHomeCurrency(context, prefHandler, userLocaleProvider)

    val accountsWithExchangeRate: String
        get() = exchangeRateJoin(TABLE_ACCOUNTS, KEY_ROWID, homeCurrency)

    val budgetTableJoin =
        "$TABLE_BUDGETS LEFT JOIN $TABLE_ACCOUNTS ON ($KEY_ACCOUNTID = $TABLE_ACCOUNTS.$KEY_ROWID)"

    private val fullAccountProjection = Account.PROJECTION_BASE + arrayOf(
        "$KEY_OPENING_BALANCE + coalesce($KEY_CURRENT,0) AS $KEY_CURRENT_BALANCE",
        KEY_SUM_INCOME,
        KEY_SUM_EXPENSES,
        KEY_SUM_TRANSFERS,
        "$KEY_OPENING_BALANCE + coalesce($KEY_TOTAL,0) AS $KEY_TOTAL",
        "$KEY_OPENING_BALANCE + coalesce($KEY_CLEARED_TOTAL,0) AS $KEY_CLEARED_TOTAL",
        "$KEY_OPENING_BALANCE + coalesce($KEY_RECONCILED_TOTAL,0) AS $KEY_RECONCILED_TOTAL",
        KEY_USAGES,
        "0 AS $KEY_IS_AGGREGATE",//this is needed in the union with the aggregates to sort real accounts first
        KEY_HAS_FUTURE,
        KEY_HAS_CLEARED,
        AccountType.sqlOrderExpression(),
        KEY_LAST_USED
    )

    val aggregateFunction: String
        get() = if (prefHandler.getBoolean(
                PrefKey.DB_SAFE_MODE,
                false
            )
        ) "total" else "sum"

    fun buildAccountQuery(
        minimal: Boolean,
        mergeAggregate: String?,
        selection: String?,
        sortOrder: String?
    ): String {

        val aggregateFunction = this.aggregateFunction

        val futureStartsNow = runBlocking {
            enumValueOrDefault(
                dataStore.data.first()[stringPreferencesKey(
                    prefHandler.getKey(
                        PrefKey.CRITERION_FUTURE
                    )
                )], FutureCriterion.EndOfDay
            )
        } == FutureCriterion.Current

        val cte = accountQueryCTE(homeCurrency, futureStartsNow, aggregateFunction)

        val joinWithAggregates =
            "$TABLE_ACCOUNTS LEFT JOIN aggregates ON $TABLE_ACCOUNTS.$KEY_ROWID = $KEY_ACCOUNTID"

        val accountQueryBuilder =
            SupportSQLiteQueryBuilder.builder(if (minimal) TABLE_ACCOUNTS else joinWithAggregates)

        val query = if (mergeAggregate == null) {
            accountQueryBuilder.columns(fullAccountProjection).selection(selection, emptyArray())
                .create().sql
        } else {
            val subQueries: MutableList<String> = ArrayList()
            if (mergeAggregate == "1") {
                subQueries.add(
                    accountQueryBuilder.columns(
                        if (minimal) arrayOf(
                            KEY_ROWID,
                            KEY_LABEL,
                            KEY_CURRENCY,
                            "0 AS $KEY_IS_AGGREGATE"
                        ) else fullAccountProjection
                    )
                        .selection(selection, emptyArray()).create().sql
                )
            }
            //Currency query
            if (mergeAggregate != Account.HOME_AGGREGATE_ID.toString()) {
                val qb = SupportSQLiteQueryBuilder.builder(
                    "$joinWithAggregates LEFT JOIN $TABLE_CURRENCIES on $KEY_CODE = $KEY_CURRENCY"
                )
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
                        "$TABLE_CURRENCIES.$KEY_SORT_DIRECTION",
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
                    qb.columns(currencyProjection)
                        .selection("$KEY_EXCLUDE_FROM_TOTALS = 0", emptyArray())
                        .groupBy(KEY_CURRENCY)
                        .having(
                            if (mergeAggregate == "1") "count(*) > 1" else "$TABLE_CURRENCIES.$KEY_ROWID = " +
                                    mergeAggregate.substring(1)
                        ).create().sql
                )
            }
            //home query
            if (mergeAggregate == Account.HOME_AGGREGATE_ID.toString() || mergeAggregate == "1") {
                val qb = SupportSQLiteQueryBuilder.builder(joinWithAggregates)

                val grouping = prefHandler.getString(AggregateAccount.GROUPING_AGGREGATE, "NONE")
                val sortDirection =
                    prefHandler.getString(AggregateAccount.SORT_DIRECTION_AGGREGATE, "DESC")
                val rowIdColumn = Account.HOME_AGGREGATE_ID.toString() + " AS " + KEY_ROWID
                val labelColumn =
                    "'${wrappedContext.getString(R.string.grand_total)}' AS $KEY_LABEL"
                val currencyColumn =
                    "'" + AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE + "' AS " + KEY_CURRENCY
                val aggregateColumn = "${AggregateAccount.AGGREGATE_HOME} AS $KEY_IS_AGGREGATE"
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
                        "'$sortDirection' AS $KEY_SORT_DIRECTION",
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
                    qb.columns(homeProjection)
                        .selection("$KEY_EXCLUDE_FROM_TOTALS = 0", emptyArray())
                        .groupBy("1")
                        .having("(select count(distinct $KEY_CURRENCY) from $TABLE_ACCOUNTS WHERE $KEY_CURRENCY != '$homeCurrency') > 0")
                        .create().sql
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
            buildUnionQuery(subQueries.toTypedArray(), "$grouping,$sortOrder")
        }
        return "$cte\n$query"
    }

    fun backup(context: Context, backupDir: File): Result<Unit> {
        val currentDb = File(helper.readableDatabase.path!!)
        helper.readableDatabase.close()
        _helper = null
        return (if (prefHandler.encryptDatabase) decrypt(currentDb, backupDir) else backupDb(currentDb, backupDir))
            .mapCatching {
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
    }

    open fun restore(backupFile: File, encrypt: Boolean): Boolean {
        val dataDir = File(getInternalAppDir(), "databases")
        dataDir.mkdir()
        val currentDb = File(dataDir, provideDatabaseName(encrypt))
        helper.close()
        val result: Boolean = try {
            if (encrypt) {
                DataModule.cryptProvider.encrypt(context!!, backupFile, currentDb)
                true
            } else {
                FileCopyUtils.copy(backupFile, currentDb)
            }
        } finally {
            prefHandler.putBoolean(PrefKey.ENCRYPT_DATABASE, encrypt)
            _helper = null
        }
        return result
    }

    fun budgetCategoryUpsert(db: SupportSQLiteDatabase, uri: Uri, values: ContentValues): Int {
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

    fun budgetDefaultSelect(
        db: SupportSQLiteDatabase,
        uri: Uri
    ): Long? {
        val accountId = uri.pathSegments[2].toLong()
        val group = uri.pathSegments[3]
        val (accountSelection, accountSelectionArg) = when {
            accountId > 0 -> "$KEY_ACCOUNTID = ?" to accountId
            accountId == AggregateAccount.HOME_AGGREGATE_ID -> "$KEY_CURRENCY = ?" to AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE
            else -> "$KEY_CURRENCY = (select $KEY_CURRENCY from $TABLE_CURRENCIES where $KEY_ROWID = ?)" to accountId
        }
        return db.query(
            TABLE_BUDGETS,
            arrayOf(KEY_ROWID),
            "$KEY_IS_DEFAULT = 1 AND $KEY_GROUPING = ? AND $accountSelection",
            arrayOf(group, accountSelectionArg)
        )
            .use { it.takeIf { it.moveToFirst() }?.getLong(0) }
    }

    fun hiddenAccountCount(db: SupportSQLiteDatabase): Bundle = Bundle(1).apply {
        putInt(
            KEY_COUNT,
            db.query("select count(*) from $TABLE_ACCOUNTS where $KEY_HIDDEN = 1").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
    }

    fun uuidForTransaction(db: SupportSQLiteDatabase, id: Long): String = db.query(
        table = TABLE_TRANSACTIONS,
        columns = arrayOf(KEY_UUID),
        selection = "$KEY_ROWID = ?",
        selectionArgs = arrayOf(id)
    ).use {
        it.moveToFirst()
        it.getString(0)
    }

    /**
     * @return number of corrupted entries
     */
    fun checkCorruptedData987() = Bundle(1).apply {
        putLongArray(KEY_RESULT, helper.readableDatabase.query(
            "select distinct transactions.parent_id from transactions left join transactions parent on transactions.parent_id = parent._id where transactions.parent_id is not null and parent.account_id != transactions.account_id",
        ).useAndMap { it.getLong(0) }.toLongArray())
    }

    private fun decrypt(currentDb: File, backupDir: File): Result<Unit> {
        val backupDb = getBackupDbFile(backupDir)
        DataModule.cryptProvider.decrypt(context!!, currentDb, backupDb)
        return ResultUnit
    }

    private fun backupDb(currentDb: File, backupDir: File): Result<Unit> {
        val backupDb = getBackupDbFile(backupDir)
        if (currentDb.exists()) {
            if (FileCopyUtils.copy(currentDb, backupDb)) {
                return ResultUnit
            }
            return Result.failure(Throwable("Error while copying ${currentDb.path} to ${backupDb.path}"))
        }
        return Result.failure(Throwable("Could not find database at ${currentDb.path}"))
    }

    @Synchronized
    private fun requireHelper(): SupportSQLiteOpenHelper {
        if (_helper == null) {
            _helper = openHelperProvider.get()
        }
        return _helper!!
    }

    fun getInternalAppDir(): File {
        return context!!.filesDir.parentFile!!
    }

    fun log(message: String, vararg args: Any) {
        Timber.tag(TAG).i(message, *args)
    }

    fun SupportSQLiteQueryBuilder.measureAndLogQuery(
        uri: Uri,
        db: SupportSQLiteDatabase,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        groupBy: String?,
        having: String?,
        sortOrder: String?,
        limit: String?
    ): Cursor {
        val query =
            columns(projection).selection(selection, selectionArgs).groupBy(groupBy).having(having)
                .orderBy(sortOrder).apply {
                    if (limit != null) {
                        limit(limit)
                    }
                }.create()
        return measure(block = {
            db.query(query)
        }) {
            "$uri - ${query.sql} - (${selectionArgs?.joinToString()})"
        }
    }

    fun updateFractionDigits(
        db: SupportSQLiteDatabase,
        currency: String,
        newValue: Int
    ): Int {
        val oldValue = currencyContext[currency].fractionDigits
        if (oldValue == newValue) {
            return 0
        }
        val bindArgs = arrayOf(currency)
        val count = db.query(
            SupportSQLiteQueryBuilder.builder(TABLE_ACCOUNTS)
                .columns(arrayOf("count(*)"))
                .selection("$KEY_CURRENCY=?", bindArgs)
                .create()
        ).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val increase: Boolean = oldValue < newValue
        val operation = if (increase) "*" else "/"
        val inverseOperation = if (increase) "/" else "*"
        val factor = 10.0.pow(abs(oldValue - newValue).toDouble()).toInt()
        safeUpdateWithSealed(db) {
            db.execSQL(
                "UPDATE $TABLE_ACCOUNTS SET $KEY_OPENING_BALANCE=$KEY_OPENING_BALANCE$operation$factor WHERE $KEY_CURRENCY=?",
                bindArgs
            )
            db.execSQL(
                "UPDATE $TABLE_TRANSACTIONS SET $KEY_AMOUNT=$KEY_AMOUNT$operation$factor WHERE $KEY_ACCOUNTID IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_CURRENCY=?)",
                bindArgs
            )
            db.execSQL(
                "UPDATE $TABLE_TEMPLATES SET $KEY_AMOUNT=$KEY_AMOUNT$operation$factor WHERE $KEY_ACCOUNTID IN (SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_CURRENCY=?)",
                bindArgs
            )
            db.execSQL(
                "UPDATE $TABLE_ACCOUNT_EXCHANGE_RATES SET $KEY_EXCHANGE_RATE=$KEY_EXCHANGE_RATE$operation$factor WHERE $KEY_CURRENCY_OTHER=?",
                bindArgs
            )
            db.execSQL(
                "UPDATE $TABLE_ACCOUNT_EXCHANGE_RATES SET $KEY_EXCHANGE_RATE=$KEY_EXCHANGE_RATE$inverseOperation$factor WHERE $KEY_CURRENCY_SELF=?",
                bindArgs
            )
            val totalBudgetClause = if (homeCurrency == currency) " OR $KEY_CURRENCY = '${AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE}'" else ""
            db.execSQL(
                """UPDATE $TABLE_BUDGET_ALLOCATIONS SET
                    $KEY_BUDGET=$KEY_BUDGET$operation$factor,
                    $KEY_BUDGET_ROLLOVER_PREVIOUS=$KEY_BUDGET_ROLLOVER_PREVIOUS$operation$factor,
                    $KEY_BUDGET_ROLLOVER_NEXT=$KEY_BUDGET_ROLLOVER_NEXT$operation$factor
                    WHERE $KEY_BUDGETID IN (
                        SELECT $KEY_ROWID FROM $TABLE_BUDGETS WHERE
                            $KEY_CURRENCY=? OR
                            $KEY_ACCOUNTID IN (
                                SELECT $KEY_ROWID FROM $TABLE_ACCOUNTS WHERE $KEY_CURRENCY=?
                            )
                            $totalBudgetClause
                    )
                """.trimIndent(),
                arrayOf(currency,currency)
            )
            db.execSQL(
                "UPDATE $TABLE_DEBTS SET $KEY_AMOUNT=$KEY_AMOUNT$operation$factor WHERE $KEY_CURRENCY=?",
                bindArgs
            )

            currencyContext.storeCustomFractionDigits(currency, newValue)
        }
        return count
    }

    fun SupportSQLiteDatabase.measureAndLogQuery(
        uri: Uri,
        selection: String?,
        sql: String,
        selectionArgs: Array<String>?
    ): Cursor = measure(block = { query(sql, selectionArgs ?: emptyArray()) }) {
        "$uri - $selection - $sql - (${selectionArgs?.joinToString()})"
    }

    private fun <T : Any> measure(block: () -> T, lazyMessage: () -> String): T = if (shouldLog) {
        val startTime = Instant.now()
        val result = block()
        val endTime = Instant.now()
        val duration = Duration.between(startTime, endTime)
        log("${lazyMessage()}\n$duration")
        result
    } else block()

    fun report(e: String) {
        CrashHandler.report(Exception(e), TAG)
    }

    override fun onCreate(): Boolean {
        MyApplication.getInstance().appComponent.inject(this)
        shouldLog = prefHandler.getBoolean(PrefKey.DEBUG_LOGGING, BuildConfig.DEBUG)
        return true
    }

    fun wrapWithResultCompat(cursor: Cursor, extras: Bundle) = when {
        extras.isEmpty -> cursor
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> cursor.apply {
            setExtras(extras)
        }
        else -> object : CursorWrapper(cursor) {
            override fun getExtras() = extras
        }
    }

    fun handleAccountProperty(
        db: SupportSQLiteDatabase,
        uri: Uri,
        key: String
    ): Int {
        val subject = uri.pathSegments[1]
        val id: Long? = subject.toLongOrNull()
        val isAggregate = id == null || id < 0
        val contentValues = ContentValues(1).apply {
            put(key, uri.pathSegments[2])
        }
        return db.update(
            if (isAggregate) TABLE_CURRENCIES else TABLE_ACCOUNTS,
            contentValues,
            (if (id == null) KEY_CODE else KEY_ROWID) + " = ?",
            arrayOf(
                if (id == null) subject else abs(id).toString()
            )
        )
    }
}