package org.totschnig.myexpenses.provider

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.text.TextUtils
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import androidx.sqlite.db.SupportSQLiteStatement
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
import timber.log.Timber
import java.io.File

fun safeUpdateWithSealed(db: SupportSQLiteDatabase, runnable: Runnable) {
    db.beginTransaction()
    try {
        ContentValues(1).apply {
            put(KEY_SEALED, -1)
            db.update(TABLE_ACCOUNTS, this, "$KEY_SEALED= ?", arrayOf("1"))
        }
        ContentValues(1).apply {
            put(KEY_SEALED, -1)
            db.update(TABLE_DEBTS, this, "$KEY_SEALED= ?", arrayOf("1"))
        }
        runnable.run()
        ContentValues(1).apply {
            put(KEY_SEALED, 1)
            db.update(TABLE_ACCOUNTS, this, "$KEY_SEALED= ?", arrayOf("-1"))
        }
        ContentValues(1).apply {
            put(KEY_SEALED, 1)
            db.update(TABLE_DEBTS, this, "$KEY_SEALED= ?", arrayOf("-1"))
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

fun linkTransfers(
    db: SupportSQLiteDatabase,
    uuid1: String,
    uuid2: String,
    writeChange: Boolean
): Int {
    db.beginTransaction()
    var count = 0
    try {
        //both transactions get uuid from first transaction
        val sql =
            "UPDATE $TABLE_TRANSACTIONS SET $KEY_CATID = null, $KEY_PAYEEID = null, $KEY_UUID = ?," +
                    "$KEY_TRANSFER_PEER = (SELECT $KEY_ROWID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?)," +
                    "$KEY_TRANSFER_ACCOUNT = (SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?) WHERE $KEY_UUID = ? AND EXISTS (SELECT 1 FROM $TABLE_TRANSACTIONS where $KEY_UUID = ?)"
        db.compileStatement(sql).use {
            it.bindAllArgsAsStrings(listOf(uuid1, uuid2, uuid2, uuid1, uuid2))
            count += it.executeUpdateDelete()
            it.bindAllArgsAsStrings(listOf(uuid1, uuid1, uuid1, uuid2, uuid1))
            count += it.executeUpdateDelete()
        }

        if (writeChange) {
            // This is a hack, we abuse the number field of the changes table for storing uuid of transfer_peer
            // We do not want to extend the table since the whole trigger based concept of recording changes
            // should be abandoned in a future new architecture of the synchronization mechanism
            val updateSql =
                "INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL, $KEY_UUID, $KEY_REFERENCE_NUMBER) " +
                        "SELECT '${TransactionChange.Type.link.name}', $KEY_ROWID, $KEY_SYNC_SEQUENCE_LOCAL, ?, ? FROM " +
                        "$TABLE_ACCOUNTS WHERE $KEY_ROWID IN ((SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?), (SELECT $KEY_TRANSFER_ACCOUNT FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?)) AND $KEY_SYNC_ACCOUNT_NAME IS NOT NULL"
            db.compileStatement(updateSql).use {
                //we write identical changes for the two accounts, so that on the other end of the synchronization we know which part of the transfer keeps its uuid
                it.bindAllArgsAsStrings(listOf(uuid1, uuid2, uuid1, uuid1))
                it.execute()
            }
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
    return count
}

fun SupportSQLiteStatement.bindAllArgsAsStrings(argsList: List<String>) {
    argsList.forEachIndexed { index, arg ->
        bindString(index + 1, arg)
    }
}

fun groupByForPaymentMethodQuery(projection: Array<String>?) =
    if (projection?.contains(KEY_ACCOUNT_TPYE_LIST) == true) KEY_ROWID else null

fun havingForPaymentMethodQuery(projection: Array<String>?) =
    if (projection?.contains(KEY_ACCOUNT_TPYE_LIST) == true) "$KEY_ACCOUNT_TPYE_LIST is not null" else null

fun tableForPaymentMethodQuery(projection: Array<String>?) =
    if (projection?.contains(KEY_ACCOUNT_TPYE_LIST) == true)
        "$TABLE_METHODS left join $TABLE_ACCOUNTTYES_METHODS on $KEY_METHODID = $KEY_ROWID"
    else
        TABLE_METHODS

fun mapPaymentMethodProjection(projection: Array<String>, ctx: Context): Array<String> {
    return projection.map { column ->
        when (column) {
            KEY_LABEL -> "${PaymentMethod.localizedLabelSqlColumn(ctx, column)} AS $column"
            KEY_TYPE -> "$TABLE_METHODS.$column"
            KEY_ACCOUNT_TPYE_LIST -> "group_concat($TABLE_ACCOUNTTYES_METHODS.$KEY_TYPE) AS $column"
            else -> column
        }
    }.toTypedArray()
}

fun SupportSQLiteDatabase.findCategory(selection: String, selectionArgs: Array<Any>) = query(
    TABLE_CATEGORIES,
    arrayOf(KEY_ROWID),
    selection,
    selectionArgs,
    null,
    null,
    null
).use {
    if (it.moveToFirst()) {
        it.getLong(0)
    } else {
        null
    }
}

fun SupportSQLiteDatabase.findSubCategory(parentId: Long, label: String) = findCategory(
    selection = "$KEY_PARENTID = ? and $KEY_LABEL = ?",
    selectionArgs = arrayOf(parentId.toString(), label)
)

fun SupportSQLiteDatabase.findMainCategory(label: String) = findCategory(
    selection = "$KEY_PARENTID is null and $KEY_LABEL = ?",
    selectionArgs = arrayOf(label)
)

fun SupportSQLiteDatabase.findCategoryByUuid(uuid: String) = findCategory(
    selection = "$KEY_UUID = ?",
    selectionArgs = arrayOf(uuid)
)

val categoryDefinitions = arrayOf(
    Triple(R.array.Cat_1, R.array.Cat_1_Icons, R.array.Cat_1_Uuids),
    Triple(R.array.Cat_2, R.array.Cat_2_Icons, R.array.Cat_2_Uuids),
    Triple(R.array.Cat_3, R.array.Cat_3_Icons, R.array.Cat_3_Uuids),
    Triple(R.array.Cat_4, R.array.Cat_4_Icons, R.array.Cat_4_Uuids),
    Triple(R.array.Cat_5, R.array.Cat_5_Icons, R.array.Cat_5_Uuids),
    Triple(R.array.Cat_6, R.array.Cat_6_Icons, R.array.Cat_6_Uuids),
    Triple(R.array.Cat_7, R.array.Cat_7_Icons, R.array.Cat_7_Uuids),
    Triple(R.array.Cat_8, R.array.Cat_8_Icons, R.array.Cat_8_Uuids),
    Triple(R.array.Cat_9, R.array.Cat_9_Icons, R.array.Cat_9_Uuids),
    Triple(R.array.Cat_10, R.array.Cat_10_Icons, R.array.Cat_10_Uuids),
    Triple(R.array.Cat_11, R.array.Cat_11_Icons, R.array.Cat_11_Uuids),
    Triple(R.array.Cat_12, R.array.Cat_12_Icons, R.array.Cat_12_Uuids),
    Triple(R.array.Cat_13, R.array.Cat_13_Icons, R.array.Cat_13_Uuids),
    Triple(R.array.Cat_14, R.array.Cat_14_Icons, R.array.Cat_14_Uuids),
    Triple(R.array.Cat_15, R.array.Cat_15_Icons, R.array.Cat_15_Uuids),
    Triple(R.array.Cat_16, R.array.Cat_16_Icons, R.array.Cat_16_Uuids),
    Triple(R.array.Cat_17, R.array.Cat_17_Icons, R.array.Cat_17_Uuids),
    Triple(R.array.Cat_18, R.array.Cat_18_Icons, R.array.Cat_18_Uuids),
    Triple(R.array.Cat_19, R.array.Cat_19_Icons, R.array.Cat_19_Uuids),
    Triple(R.array.Cat_20, R.array.Cat_20_Icons, R.array.Cat_20_Uuids),
    Triple(R.array.Cat_21, R.array.Cat_21_Icons, R.array.Cat_21_Uuids),
    Triple(R.array.Cat_22, R.array.Cat_22_Icons, R.array.Cat_22_Uuids),
)

fun setupDefaultCategories(database: SupportSQLiteDatabase, resources: Resources): Pair<Int, Int> {

    var totalInserted = 0
    var totalUpdated = 0
    var catIdMain: Long?
    database.beginTransaction()
    val stmt = database.compileStatement(
        "INSERT INTO $TABLE_CATEGORIES ($KEY_LABEL, $KEY_LABEL_NORMALIZED, $KEY_PARENTID, $KEY_COLOR, $KEY_ICON, $KEY_UUID) VALUES (?, ?, ?, ?, ?, ?)"
    )
    val stmtUpdateIcon = database.compileStatement(
        "UPDATE $TABLE_CATEGORIES SET $KEY_ICON = ? WHERE $KEY_ICON IS NULL AND $KEY_ROWID = ?"
    )

    for ((categoriesResId, iconsResId, uuidResId) in categoryDefinitions) {
        val categories = resources.getStringArray(categoriesResId)
        val icons = resources.getStringArray(iconsResId)
        val uuids = resources.getStringArray(uuidResId)
        if (categories.size != icons.size || categories.size != uuids.size) {
            CrashHandler.report(Exception("Inconsistent category definitions"))
            continue
        }
        val mainLabel = categories[0]
        val mainIcon = icons[0]
        val mainUUid = uuids[0]
        catIdMain = database.findMainCategory(mainLabel) ?: database.findCategoryByUuid(mainUUid)
        if (catIdMain != null) {
            Timber.i("category with label %s already defined", mainLabel)
            stmtUpdateIcon.bindString(1, mainIcon)
            stmtUpdateIcon.bindLong(2, catIdMain)
            totalUpdated += stmtUpdateIcon.executeUpdateDelete()
        } else {
            stmt.bindString(1, mainLabel)
            stmt.bindString(2, Utils.normalize(mainLabel))
            stmt.bindNull(3)
            stmt.bindLong(4, suggestNewCategoryColor(database).toLong())
            stmt.bindString(5, mainIcon)
            stmt.bindString(6, mainUUid)
            catIdMain = stmt.executeInsert().takeIf { it != -1L }
            if (catIdMain != null) {
                totalInserted++
            } else {
                // this should not happen
                Timber.w("could neither retrieve nor store main category %s", mainLabel)
                continue
            }
        }
        val subLabels = categories.drop(1)
        val subIconNames = icons.drop(1)
        val subUuids = uuids.drop(1)
        for (i in subLabels.indices) {
            val subLabel = subLabels[i]
            val subIcon = subIconNames[i]
            val subUUid = subUuids[i]
            val catIdSub = database.findSubCategory(catIdMain, subLabel) ?: database.findCategoryByUuid(subUUid)
            if (catIdSub != null) {
                Timber.i("category with label %s already defined", subLabel)
                stmtUpdateIcon.bindString(1, subIcon)
                stmtUpdateIcon.bindLong(2, catIdSub)
                totalUpdated += stmtUpdateIcon.executeUpdateDelete()
            } else {
                stmt.bindString(1, subLabel)
                stmt.bindString(2, Utils.normalize(subLabel))
                stmt.bindLong(3, catIdMain)
                stmt.bindNull(4)
                stmt.bindString(5, subIcon)
                stmt.bindString(6, subUUid)
                try {
                    if (stmt.executeInsert() != -1L) {
                        totalInserted++
                    } else {
                        Timber.i("could not store sub category %s", subLabel)
                    }
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.report(e)
                }
            }
        }
    }
    stmt.close()
    stmtUpdateIcon.close()
    database.setTransactionSuccessful()
    database.endTransaction()
    return totalInserted to totalUpdated
}

fun insertUuidsForDefaultCategories(database: SupportSQLiteDatabase, resources: Resources) {
    database.compileStatement(
        "UPDATE $TABLE_CATEGORIES SET $KEY_UUID = ? WHERE $KEY_UUID IS NULL AND $KEY_ROWID = ?"
    ).use { statement ->
        var catIdMain: Long?
        for ((categoriesResId, _, uuidResId) in categoryDefinitions) {
            val categories = resources.getStringArray(categoriesResId)
            val uuids = resources.getStringArray(uuidResId)
            if (categories.size != uuids.size) {
                CrashHandler.report(Exception("Inconsistent category definitions"))
                continue
            }
            val mainLabel = categories[0]
            val mainUUid = uuids[0]
            catIdMain = database.findMainCategory(mainLabel)
            if (catIdMain != null) {
                statement.bindString(1, mainUUid)
                statement.bindLong(2, catIdMain)
                statement.executeUpdateDelete()
            }
            val subLabels = categories.drop(1)
            val subUuids = uuids.drop(1)
            if (catIdMain != null) {
                for (i in subLabels.indices) {
                    val subLabel = subLabels[i]
                    val subUUid = subUuids[i]
                    val catIdSub = database.findSubCategory(catIdMain, subLabel)
                    if (catIdSub != null) {
                        statement.bindString(1, subUUid)
                        statement.bindLong(2, catIdSub)
                        statement.executeUpdateDelete()
                    }
                }
            }
        }
    }
}

fun <T> Cursor.useAndMap(mapper: (Cursor) -> T) =
    use {
        generateSequence { takeIf { it.moveToNext() } }.map(mapper).toList()
    }

/**
 * requires the Cursor to be positioned BEFORE first row
 */
val Cursor.asSequence: Sequence<Cursor>
    get() {
        check(isBeforeFirst)
        return generateSequence { takeIf { it.moveToNext() } }
    }

fun Cursor.requireString(columnIndex: Int) = getStringOrNull(columnIndex) ?: ""
fun Cursor.getString(column: String) = requireString(getColumnIndexOrThrow(column))
fun Cursor.getInt(column: String) = getInt(getColumnIndexOrThrow(column))
fun Cursor.getLong(column: String) = getLong(getColumnIndexOrThrow(column))
fun Cursor.getDouble(column: String) = getDouble(getColumnIndexOrThrow(column))
fun Cursor.getStringOrNull(column: String) =
    getStringOrNull(getColumnIndexOrThrow(column))?.takeIf { it.isNotEmpty() }

fun Cursor.getIntOrNull(column: String) = getIntOrNull(getColumnIndexOrThrow(column))
fun Cursor.getLongOrNull(column: String) = getLongOrNull(getColumnIndexOrThrow(column))
fun Cursor.requireLong(column: String) = getLongOrNull(getColumnIndexOrThrow(column)) ?: 0L
fun Cursor.getIntIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getInt(it) }

fun Cursor.getIntIfExistsOr0(column: String) = getIntIfExists(column) ?: 0
fun Cursor.getLongIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getLongOrNull(it) }

fun Cursor.getLongIfExistsOr0(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getLong(it) } ?: 0L

fun Cursor.getStringIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getString(it) }

fun Cursor.getDoubleIfExists(column: String) =
    getColumnIndex(column).takeIf { it != -1 }?.let { getDouble(it) }

fun Cursor.getBoolean(column: String) = getInt(column) == 1
fun Cursor.getBoolean(columnIndex: Int) = getInt(columnIndex) == 1

inline fun <reified T : Enum<T>> Cursor.getEnum(column: String, default: T) =
    enumValueOrDefault(getString(column), default)

/**
 * Splits the value of column by ASCII UnitSeparator char
 */
fun Cursor.splitStringList(colum: String) = getString(colum)
    .takeIf { it.isNotEmpty() }
    ?.split('')
    ?: emptyList()

fun cacheSyncState(context: Context) {
    val accountManager = AccountManager.get(context)
    context.contentResolver.query(
        TransactionProvider.ACCOUNTS_URI,
        arrayOf(KEY_ROWID, KEY_SYNC_ACCOUNT_NAME),
        "$KEY_SYNC_ACCOUNT_NAME IS NOT null",
        null,
        null
    )?.use {
        val editor = (context.applicationContext as MyApplication).settings.edit()
        if (it.moveToFirst()) {
            do {
                val accountId = it.getLong(0)
                val accountName = it.getString(1)
                val localKey = SyncAdapter.KEY_LAST_SYNCED_LOCAL(accountId)
                val remoteKey = SyncAdapter.KEY_LAST_SYNCED_REMOTE(accountId)
                val account = getAccount(accountName)
                try {
                    val localValue = accountManager.getUserData(account, localKey)
                    val remoteValue = accountManager.getUserData(account, remoteKey)
                    editor.putString(localKey, localValue)
                    editor.putString(remoteKey, remoteValue)
                } catch (e: SecurityException) {
                    break
                }
            } while (it.moveToNext())
            editor.apply()
        }
    }
}

const val CALENDAR_FULL_PATH_PROJECTION: String = ("ifnull("
        + Calendars.ACCOUNT_NAME + ",'') || '/' ||" + "ifnull("
        + Calendars.ACCOUNT_TYPE + ",'') || '/' ||" + "ifnull(" + Calendars.NAME
        + ",'')")

fun getCalendarPath(contentResolver: ContentResolver, calendarId: String) =
    contentResolver.query(
        Calendars.CONTENT_URI,
        arrayOf("$CALENDAR_FULL_PATH_PROJECTION AS path"),
        Calendars._ID + " = ?",
        arrayOf(calendarId),
        null
    )?.use { if (it.moveToFirst()) it.requireString(0) else "" }

fun cacheEventData(context: Context, prefHandler: PrefHandler) {
    if (!PermissionGroup.CALENDAR.hasPermission(context)) {
        return
    }
    val plannerCalendarId = prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID, "-1")
    if (plannerCalendarId == "-1") {
        return
    }
    val eventValues = ContentValues()
    val cr = context.contentResolver
    //remove old cache
    cr.delete(
        TransactionProvider.EVENT_CACHE_URI, null, null
    )
    cr.query(
        Template.CONTENT_URI, arrayOf(
            KEY_PLANID
        ),
        "$KEY_PLANID IS NOT null", null, null
    )?.use { planCursor ->
        if (planCursor.moveToFirst()) {
            val projection = MyApplication.buildEventProjection()
            do {
                val planId = planCursor.getLong(0)
                val eventUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    planId
                )
                cr.query(
                    eventUri, projection,
                    CalendarContract.Events.CALENDAR_ID + " = ?", arrayOf(plannerCalendarId), null
                )?.use { eventCursor ->
                    if (eventCursor.moveToFirst()) {
                        MyApplication.copyEventData(eventCursor, eventValues)
                        cr.insert(TransactionProvider.EVENT_CACHE_URI, eventValues)
                    }
                }
            } while (planCursor.moveToNext())
        }
    }
}

fun SupportSQLiteDatabase.update(
    table: String,
    values: ContentValues,
    whereClause: String?,
    whereArgs: Array<Any>?
) = //https://github.com/sqlcipher/android-database-sqlcipher/issues/615
    update(table, SQLiteDatabase.CONFLICT_NONE, values, whereClause, whereArgs ?: emptyArray())

fun SupportSQLiteDatabase.insert(table: String, values: ContentValues): Long =
    insert(table, SQLiteDatabase.CONFLICT_NONE, values)

fun SupportSQLiteDatabase.query(
    table: String,
    columns: Array<String>,
    selection: String? = null,
    selectionArgs: Array<Any>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null
): Cursor = query(
    SupportSQLiteQueryBuilder.builder(table)
        .columns(columns)
        .selection(selection, selectionArgs)
        .groupBy(groupBy)
        .having(having)
        .orderBy(orderBy)
        .apply {
            if (limit != null) {
                limit(limit)
            }
        }
        .create())

fun suggestNewCategoryColor(db: SupportSQLiteDatabase) = db.query(
    table = ColorUtils.MAIN_COLORS_AS_TABLE,
    columns = arrayOf(
        "color",
        "(select count(*) from categories where parent_id is null and color=t.color) as count"
    ),
    orderBy = "count ASC",
    limit = "1"
).use {
    it.moveToFirst()
    it.getInt(0)
}

fun buildUnionQuery(
    subQueries: Array<String?>,
    sortOrder: String? = null,
    limit: String? = null,
    distinct: Boolean = false
): String = subQueries.joinToString(if (distinct) " UNION " else " UNION ALL ") +
        (if (sortOrder != null) " ORDER BY $sortOrder" else "") +
        if (limit != null) " LIMIT $limit" else ""

fun computeWhere(selection: String?, whereClause: java.lang.StringBuilder): String? {
    val hasInternal = !TextUtils.isEmpty(whereClause)
    val hasExternal = !TextUtils.isEmpty(selection)
    return if (hasInternal || hasExternal) {
        val where = StringBuilder()
        if (hasInternal) {
            where.append('(').append(whereClause).append(')')
        }
        if (hasInternal && hasExternal) {
            where.append(" AND ")
        }
        if (hasExternal) {
            where.append('(').append(selection).append(')')
        }
        where.toString()
    } else {
        null
    }
}

fun backup(backupDir: File, context: Context, prefHandler: PrefHandler): Result<Unit> {
    cacheEventData(context, prefHandler)
    cacheSyncState(context)
    return with(context.contentResolver.acquireContentProviderClient(TransactionProvider.AUTHORITY)!!) {
        try {
            (localContentProvider as BaseTransactionProvider).backup(context, backupDir)
        } finally {
            release()
        }
    }
}

fun maybeRepairRequerySchema(path: String) {
    val version = io.requery.android.database.sqlite.SQLiteDatabase.openDatabase(
        path,
        null,
        io.requery.android.database.sqlite.SQLiteDatabase.OPEN_READONLY
    ).use {
        it.version
    }
    if (version == 132 || version == 133) {
        doRepairRequerySchema(path)
    }
}

fun doRepairRequerySchema(path: String) {
    Timber.w("Dropping views with requery")
    io.requery.android.database.sqlite.SQLiteDatabase.openDatabase(
        path,
        null,
        io.requery.android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
    ).use { db ->
        if (db.version < 132 || db.version > 133) throw IllegalStateException()
        db.execSQL("DROP VIEW IF EXISTS $VIEW_COMMITTED")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_UNCOMMITTED")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_ALL")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_EXTENDED")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_CHANGES_EXTENDED")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_WITH_ACCOUNT")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_TEMPLATES_ALL")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_TEMPLATES_EXTENDED")
        db.execSQL("DROP VIEW IF EXISTS $VIEW_TEMPLATES_UNCOMMITTED")
    }
}

fun checkSyncAccounts(context: Context) {
    val validAccounts = getAccountNames(context)
    val where =
        if (validAccounts.isNotEmpty())
            "$KEY_SYNC_ACCOUNT_NAME NOT " + WhereFilter.Operation.IN.getOp(validAccounts.size)
        else null
    context.contentResolver.update(
        TransactionProvider.ACCOUNTS_URI, ContentValues(1).apply {
            putNull(KEY_SYNC_ACCOUNT_NAME)
        },
        where, validAccounts
    )
}