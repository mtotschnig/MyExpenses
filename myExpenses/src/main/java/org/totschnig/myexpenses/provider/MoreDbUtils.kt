package org.totschnig.myexpenses.provider

import android.accounts.AccountManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.io.File

fun safeUpdateWithSealed(db: SQLiteDatabase, runnable: Runnable) {
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

fun linkTransfers(db: SQLiteDatabase, uuid1: String, uuid2: String, writeChange: Boolean): Int {
    db.beginTransaction()
    var count = 0
    try {
        //both transactions get uuid from first transaction
        val sql =
            "UPDATE $TABLE_TRANSACTIONS SET $KEY_CATID = null, $KEY_PAYEEID = null, $KEY_UUID = ?," +
                    "$KEY_TRANSFER_PEER = (SELECT $KEY_ROWID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?)," +
                    "$KEY_TRANSFER_ACCOUNT = (SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?) WHERE $KEY_UUID = ? AND EXISTS (SELECT 1 FROM $TABLE_TRANSACTIONS where $KEY_UUID = ?)"
        val statement: SQLiteStatement = db.compileStatement(sql)
        statement.bindAllArgsAsStrings(arrayOf(uuid1, uuid2, uuid2, uuid1, uuid2))
        count += statement.executeUpdateDelete()
        statement.bindAllArgsAsStrings(arrayOf(uuid1, uuid1, uuid1, uuid2, uuid1))
        count += statement.executeUpdateDelete()
        if (writeChange) {
            // This is a hack, we abuse the number field of the changes table for storing uuid of transfer_peer
            // We do not want to extend the table since the whole trigger based concept of recording changes
            // should be abandoned in a future new architecture of the synchronization mechanism
            val updateSql =
                "INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL, $KEY_UUID, $KEY_REFERENCE_NUMBER) " +
                        "SELECT '${TransactionChange.Type.link.name}', $KEY_ROWID, $KEY_SYNC_SEQUENCE_LOCAL, ?, ? FROM " +
                        "$TABLE_ACCOUNTS WHERE $KEY_ROWID IN ((SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?), (SELECT $KEY_TRANSFER_ACCOUNT FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?)) AND $KEY_SYNC_ACCOUNT_NAME IS NOT NULL"
            val updateStatement: SQLiteStatement = db.compileStatement(updateSql)
            //we write identical changes for the two accounts, so that on the other end of the synchronization we know which part of the transfer keeps its uuid
            updateStatement.bindAllArgsAsStrings(arrayOf(uuid1, uuid2, uuid1, uuid1))
            updateStatement.execute()
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
    return count
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

fun setupDefaultCategories(database: SQLiteDatabase, resources: Resources): Pair<Int, Int> {
    var totalInserted = 0
    var totalUpdated = 0
    var catIdMain: Long
    database.beginTransaction()
    val stmt = database.compileStatement(
        "INSERT INTO $TABLE_CATEGORIES ($KEY_LABEL, $KEY_LABEL_NORMALIZED, $KEY_PARENTID, $KEY_COLOR, $KEY_ICON) VALUES (?, ?, ?, ?, ?)"
    )
    val stmtUpdateIcon = database.compileStatement(
        "UPDATE $TABLE_CATEGORIES SET $KEY_ICON = ? WHERE $KEY_ICON IS NULL AND $KEY_ROWID = ?"
    )

    val categoryDefinitions = arrayOf(
        R.array.Cat_1 to R.array.Cat_1_Icons,
        R.array.Cat_2 to R.array.Cat_2_Icons,
        R.array.Cat_3 to R.array.Cat_3_Icons,
        R.array.Cat_4 to R.array.Cat_4_Icons,
        R.array.Cat_5 to R.array.Cat_5_Icons,
        R.array.Cat_6 to R.array.Cat_6_Icons,
        R.array.Cat_7 to R.array.Cat_7_Icons,
        R.array.Cat_8 to R.array.Cat_8_Icons,
        R.array.Cat_9 to R.array.Cat_9_Icons,
        R.array.Cat_10 to R.array.Cat_10_Icons,
        R.array.Cat_11 to R.array.Cat_11_Icons,
        R.array.Cat_12 to R.array.Cat_12_Icons,
        R.array.Cat_13 to R.array.Cat_13_Icons,
        R.array.Cat_14 to R.array.Cat_14_Icons,
        R.array.Cat_15 to R.array.Cat_15_Icons,
        R.array.Cat_16 to R.array.Cat_16_Icons,
        R.array.Cat_17 to R.array.Cat_17_Icons,
        R.array.Cat_18 to R.array.Cat_18_Icons,
        R.array.Cat_19 to R.array.Cat_19_Icons,
        R.array.Cat_20 to R.array.Cat_20_Icons,
        R.array.Cat_21 to R.array.Cat_21_Icons,
        R.array.Cat_22 to R.array.Cat_22_Icons
    )

    for ((categoriesResId, iconsResId) in categoryDefinitions) {
        val categories = resources.getStringArray(categoriesResId)
        val icons = resources.getStringArray(iconsResId)
        if (categories.size != icons.size) {
            CrashHandler.report(Exception("Inconsistent category definitions"))
            continue
        }
        val mainLabel = categories[0]
        val mainIcon = icons[0]
        catIdMain = findMainCategory(database, mainLabel)
        if (catIdMain != -1L) {
            Timber.i("category with label %s already defined", mainLabel)
            stmtUpdateIcon.bindString(1, mainIcon)
            stmtUpdateIcon.bindLong(2, catIdMain)
            totalUpdated += stmtUpdateIcon.executeUpdateDelete()
        } else {
            stmt.bindString(1, mainLabel)
            stmt.bindString(2, Utils.normalize(mainLabel))
            stmt.bindNull(3)
            stmt.bindLong(4, DbUtils.suggestNewCategoryColor(database).toLong())
            stmt.bindString(5, mainIcon)
            catIdMain = stmt.executeInsert()
            if (catIdMain != -1L) {
                totalInserted++
            } else {
                // this should not happen
                Timber.w("could neither retrieve nor store main category %s", mainLabel)
                continue
            }
        }
        val subLabels = categories.drop(1)
        val subIconNames = icons.drop(1)
        for (i in subLabels.indices) {
            val subLabel = subLabels[i]
            val subIcon = subIconNames[i]
            val catIdSub = findSubCategory(database, catIdMain, subLabel)
            if (catIdSub != -1L) {
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

private fun findCategory(database: SQLiteDatabase, selection: String, selectionArgs: Array<String>) =
    database.query(
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
            -1
        }
    }

private fun findSubCategory(database: SQLiteDatabase, parentId: Long, label: String) =
    findCategory(database, "$KEY_PARENTID = ? and $KEY_LABEL = ?" , arrayOf(parentId.toString(), label))

private fun findMainCategory(database: SQLiteDatabase, label: String) =
    findCategory(database, "$KEY_PARENTID is null and $KEY_LABEL = ?" , arrayOf(label))

/**
 * requires the Cursor to be positioned BEFORE first row
 */
val Cursor.asSequence: Sequence<Cursor>
    get() = generateSequence { takeIf { it.moveToNext() } }

fun Cursor.getString(column: String) = getStringOrNull(getColumnIndexOrThrow(column)) ?: ""
fun Cursor.getInt(column: String) = getInt(getColumnIndexOrThrow(column))
fun Cursor.getLong(column: String) = getLong(getColumnIndexOrThrow(column))
fun Cursor.getStringOrNull(column: String) = getStringOrNull(getColumnIndexOrThrow(column))
fun Cursor.getIntOrNull(column: String) = getIntOrNull(getColumnIndexOrThrow(column))
fun Cursor.getLongOrNull(column: String) = getLongOrNull(getColumnIndexOrThrow(column))

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

fun cacheEventData(context: Context, prefHandler: PrefHandler) {
    if (!PermissionGroup.CALENDAR.hasPermission(context)) {
        return
    }
    val plannerCalendarId = prefHandler.getString(PrefKey.PLANNER_CALENDAR_ID,"-1")
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
        KEY_PLANID + " IS NOT null", null, null
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