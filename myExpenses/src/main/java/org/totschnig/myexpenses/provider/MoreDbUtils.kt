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
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import androidx.sqlite.db.SupportSQLiteStatement
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.DEFAULT_CATEGORY_PATH_SEPARATOR
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.localizedLabelForPaymentMethod
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.myApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE_LIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTTYES_METHODS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_TYPES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CHANGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.PlannerUtils.Companion.copyEventData
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccountNames
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Category
import timber.log.Timber
import java.io.File

fun <T> SupportSQLiteDatabase.safeUpdateWithSealed(runnable: () -> T): T {
    beginTransaction()
    try {
        ContentValues(1).apply {
            put(KEY_SEALED, -1)
            update(TABLE_ACCOUNTS, this, "$KEY_SEALED= ?", arrayOf("1"))
        }
        ContentValues(1).apply {
            put(KEY_SEALED, -1)
            update(TABLE_DEBTS, this, "$KEY_SEALED= ?", arrayOf("1"))
        }
        val result = runnable()
        ContentValues(1).apply {
            put(KEY_SEALED, 1)
            update(TABLE_ACCOUNTS, this, "$KEY_SEALED= ?", arrayOf("-1"))
        }
        ContentValues(1).apply {
            put(KEY_SEALED, 1)
            update(TABLE_DEBTS, this, "$KEY_SEALED= ?", arrayOf("-1"))
        }
        setTransactionSuccessful()
        return result
    } finally {
        endTransaction()
    }
}

fun unlinkTransfers(
    db: SupportSQLiteDatabase,
    id: String,
): Int {
    db.beginTransaction()
    try {
        val result1 = db.update(TABLE_TRANSACTIONS, ContentValues().apply {
            putNull(KEY_TRANSFER_PEER)
            putNull(KEY_TRANSFER_ACCOUNT)
            put(KEY_UUID, Model.generateUuid())
        }, "$KEY_ROWID = ?", arrayOf(id))
        check(result1 == 1) {
            "Update by rowId yielded $result1 affected rows"
        }
        val result2 = db.update(TABLE_TRANSACTIONS, ContentValues().apply {
            putNull(KEY_TRANSFER_PEER)
            putNull(KEY_TRANSFER_ACCOUNT)
            put(KEY_UUID, Model.generateUuid())
        }, "$KEY_TRANSFER_PEER = ?", arrayOf(id))
        check(result2 == 1) {
            "Update by transferPeer yielded $result2 affected rows"
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
    return 2
}

fun linkTransfers(
    db: SupportSQLiteDatabase,
    uuid1: String,
    uuid2: String,
    writeChange: Boolean,
): Int {
    db.beginTransaction()
    try {
        //both transactions get uuid from first transaction
        val sql = """UPDATE $TABLE_TRANSACTIONS SET
            $KEY_CATID = null,
            $KEY_PAYEEID = null,
            $KEY_UUID = ?,
            $KEY_TRANSFER_PEER = (SELECT $KEY_ROWID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?),
            $KEY_TRANSFER_ACCOUNT = (SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?) 
            WHERE $KEY_UUID = ? AND EXISTS (SELECT 1 FROM $TABLE_TRANSACTIONS where $KEY_UUID = ?)"""
        db.compileStatement(sql).use {
            it.bindAllArgsAsStrings(listOf(uuid1, uuid2, uuid2, uuid1, uuid2))
            val result1 = it.executeUpdateDelete()
            check(result1 == 1) {
                "Update for $uuid1 yielded $result1 affected rows"
            }
            it.bindAllArgsAsStrings(listOf(uuid1, uuid1, uuid1, uuid2, uuid1))
            val result2 = it.executeUpdateDelete()
            check(result2 == 1) {
                "Update for $uuid2 yielded $result2 affected rows"
            }
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
    return 2
}

fun transformToTransfer(
    db: SupportSQLiteDatabase,
    uri: Uri,
    defaultTransferCategory: Long?,
): Long {
    val (transactionId, transferAccountId) = with(uri.pathSegments) {
        get(1) to get(3)
    }
    db.beginTransaction()
    return try {
        //insert transfer peer into transfer account
        val transferPeer = db.compileStatement(
            """INSERT INTO $TABLE_TRANSACTIONS ($KEY_ACCOUNTID, $KEY_TRANSFER_ACCOUNT, $KEY_UUID, $KEY_TRANSFER_PEER, $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_CATID)
            SELECT $transferAccountId, $KEY_ACCOUNTID, $KEY_UUID, $transactionId, $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, -$KEY_AMOUNT, coalesce($KEY_CATID, ?) FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = ?
            """
        ).use {
            if (defaultTransferCategory != null) {
                it.bindLong(1, defaultTransferCategory)
            } else {
                it.bindNull(1)
            }
            it.bindString(2, transactionId)
            it.executeInsert()
        }
        //update original transaction
        val updateCount = db.compileStatement(
            """UPDATE $TABLE_TRANSACTIONS SET $KEY_TRANSFER_ACCOUNT = ?, $KEY_TRANSFER_PEER = ?, $KEY_METHODID = null, $KEY_CATID = coalesce($KEY_CATID, ?) WHERE $KEY_ROWID = ?"""
        ).use {
            it.bindString(1, transferAccountId)
            it.bindLong(2, transferPeer)
            if (defaultTransferCategory != null) {
                it.bindLong(3, defaultTransferCategory)
            } else {
                it.bindNull(3)
            }
            it.bindString(4, transactionId)
            it.executeUpdateDelete()
        }
        check(updateCount == 1)
        db.setTransactionSuccessful()
        transferPeer
    } finally {
        db.endTransaction()
    }
}

fun SupportSQLiteStatement.bindAllArgsAsStrings(argsList: List<String>) {
    argsList.forEachIndexed { index, arg ->
        bindString(index + 1, arg)
    }
}

fun groupByForPaymentMethodQuery(projection: Array<String>?) =
    if (projection?.contains(KEY_ACCOUNT_TYPE_LIST) == true) KEY_ROWID else null

fun havingForPaymentMethodQuery(projection: Array<String>?) =
    if (projection?.contains(KEY_ACCOUNT_TYPE_LIST) == true) "$KEY_ACCOUNT_TYPE_LIST is not null" else null

fun tableForPaymentMethodQuery(projection: Array<String>?) =
    if (projection?.contains(KEY_ACCOUNT_TYPE_LIST) == true)
        "$TABLE_METHODS left join $TABLE_ACCOUNTTYES_METHODS on $KEY_METHODID = $KEY_ROWID"
    else
        TABLE_METHODS

fun mapPaymentMethodProjection(projection: Array<String>, ctx: Context) =
    projection.map { column ->
        when (column) {
            KEY_LABEL -> "${localizedLabelForPaymentMethod(ctx, column)} AS $column"
            KEY_TYPE -> "$TABLE_METHODS.$column"
            KEY_ACCOUNT_TYPE_LIST -> "group_concat($TABLE_ACCOUNTTYES_METHODS.$KEY_TYPE) AS $column"
            else -> column
        }
    }.toTypedArray()

fun mapAccountProjection(projection: Array<String>?) =
    projection?.map { column ->
        when (column) {
            KEY_LABEL, KEY_DESCRIPTION, KEY_ROWID, KEY_CURRENCY, KEY_GROUPING -> "$TABLE_ACCOUNTS.$column AS $column"
            KEY_ACCOUNT_TYPE_LABEL -> "$TABLE_ACCOUNT_TYPES.$KEY_LABEL AS $column"
            else -> column
        }
    }?.toTypedArray()

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


val incomeCategories = arrayOf(
    Triple(R.array.Cat_I_1, R.array.Cat_I_1_Icons, R.array.Cat_I_1_Uuids),
    Triple(R.array.Cat_I_2, R.array.Cat_I_2_Icons, R.array.Cat_I_2_Uuids),
    Triple(R.array.Cat_I_3, R.array.Cat_I_3_Icons, R.array.Cat_I_3_Uuids),
    Triple(R.array.Cat_I_4, R.array.Cat_I_4_Icons, R.array.Cat_I_4_Uuids),
    Triple(R.array.Cat_I_5, R.array.Cat_I_5_Icons, R.array.Cat_I_5_Uuids)
)

val expenseCategories = arrayOf(
    Triple(R.array.Cat_E_1, R.array.Cat_E_1_Icons, R.array.Cat_E_1_Uuids),
    Triple(R.array.Cat_E_2, R.array.Cat_E_2_Icons, R.array.Cat_E_2_Uuids),
    Triple(R.array.Cat_E_3, R.array.Cat_E_3_Icons, R.array.Cat_E_3_Uuids),
    Triple(R.array.Cat_E_4, R.array.Cat_E_4_Icons, R.array.Cat_E_4_Uuids),
    Triple(R.array.Cat_E_5, R.array.Cat_E_5_Icons, R.array.Cat_E_5_Uuids),
    Triple(R.array.Cat_E_6, R.array.Cat_E_6_Icons, R.array.Cat_E_6_Uuids),
    Triple(R.array.Cat_E_7, R.array.Cat_E_7_Icons, R.array.Cat_E_7_Uuids),
    Triple(R.array.Cat_E_8, R.array.Cat_E_8_Icons, R.array.Cat_E_8_Uuids),
    Triple(R.array.Cat_E_9, R.array.Cat_E_9_Icons, R.array.Cat_E_9_Uuids),
    Triple(R.array.Cat_E_10, R.array.Cat_E_10_Icons, R.array.Cat_E_10_Uuids),
    Triple(R.array.Cat_E_11, R.array.Cat_E_11_Icons, R.array.Cat_E_11_Uuids),
    Triple(R.array.Cat_E_12, R.array.Cat_E_12_Icons, R.array.Cat_E_12_Uuids),
    Triple(R.array.Cat_E_13, R.array.Cat_E_13_Icons, R.array.Cat_E_13_Uuids),
    Triple(R.array.Cat_E_14, R.array.Cat_E_14_Icons, R.array.Cat_E_14_Uuids),
    Triple(R.array.Cat_E_15, R.array.Cat_E_15_Icons, R.array.Cat_E_15_Uuids),
    Triple(R.array.Cat_E_16, R.array.Cat_E_16_Icons, R.array.Cat_E_16_Uuids),
    Triple(R.array.Cat_E_17, R.array.Cat_E_17_Icons, R.array.Cat_E_17_Uuids)
)

fun getImportableCategories(
    database: SupportSQLiteDatabase,
    resources: Resources,
) = Category(
    children = (incomeCategories + expenseCategories)
        .mapIndexedNotNull { indexMain, (categoriesResId, _, uuidResId) ->
            val categories = resources.getStringArray(categoriesResId)
            val uuids = resources.getStringArray(uuidResId)
            if (categories.size != uuids.size) {
                CrashHandler.report(Exception("Inconsistent category definitions"))
                null
            } else {
                val mainLabel = categories[0]
                val mainUUid = uuids[0]
                val catIdMain = database.findMainCategory(mainLabel)
                    ?: database.findCategoryByUuid(mainUUid)
                val mainPath = if (catIdMain == null) mainLabel else
                    database.query(categoryPathFromLeave(catIdMain.toString())).use { cursor ->
                        cursor.asSequence.map { it.getString(KEY_LABEL) }.toList().asReversed()
                    }.joinToString(DEFAULT_CATEGORY_PATH_SEPARATOR)
                val subCategories = categories.drop(1).zip(uuids.drop(1))
                    .mapIndexedNotNull { indexSub, (subLabel, subUuid) ->
                        if ((catIdMain?.let { main ->
                                database.findSubCategory(main, subLabel)
                            } ?: database.findCategoryByUuid(subUuid)) == null) {
                            Category(
                                id = indexMain * 100L + indexSub + 1,
                                parentId = indexMain.toLong(),
                                label = subLabel,
                                level = 2
                            )
                        } else null
                    }
                if (catIdMain != null && subCategories.isEmpty()) null else Category(
                    id = indexMain.toLong() * 100L,
                    label = mainPath,
                    children = subCategories,
                    level = 1
                )
            }
        })

private fun setupCategoriesInternal(
    database: SupportSQLiteDatabase,
    resources: Resources,
    categoryDefinitions: Array<Triple<Int, Int, Int>>,
    typeFlag: Byte,
): Pair<Int, Int> {
    var totalInserted = 0
    var totalUpdated = 0
    var catIdMain: Long?
    database.beginTransaction()
    val stmt = database.compileStatement(
        "INSERT INTO $TABLE_CATEGORIES ($KEY_LABEL, $KEY_LABEL_NORMALIZED, $KEY_PARENTID, $KEY_COLOR, $KEY_ICON, $KEY_UUID, $KEY_TYPE) VALUES (?, ?, ?, ?, ?, ?, ?)"
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
            stmt.bindLong(7, typeFlag.toLong())
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
            val catIdSub = database.findSubCategory(catIdMain, subLabel)
                ?: database.findCategoryByUuid(subUUid)
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
                stmt.bindNull(7)
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

fun setupDefaultCategories(database: SupportSQLiteDatabase, resources: Resources): Pair<Int, Int> {
    val expense = setupCategoriesInternal(database, resources, expenseCategories, FLAG_EXPENSE)
    val income = setupCategoriesInternal(database, resources, incomeCategories, FLAG_INCOME)
    return expense.first + income.first to expense.second + income.second
}

fun insertUuidsForDefaultCategories(database: SupportSQLiteDatabase, resources: Resources) {
    database.compileStatement(
        "UPDATE $TABLE_CATEGORIES SET $KEY_UUID = ? WHERE $KEY_UUID IS NULL AND $KEY_ROWID = ?"
    ).use { statement ->
        var catIdMain: Long?
        for ((categoriesResId, _, uuidResId) in expenseCategories + incomeCategories) {
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

fun cacheSyncState(context: Context) {
    val accountManager = AccountManager.get(context)
    context.contentResolver.query(
        TransactionProvider.ACCOUNTS_URI,
        arrayOf(KEY_ROWID, KEY_SYNC_ACCOUNT_NAME),
        "$KEY_SYNC_ACCOUNT_NAME IS NOT null",
        null,
        null
    )?.use {
        val editor = context.myApplication.settings.edit()
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
                } catch (_: SecurityException) {
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
            do {
                val planId = planCursor.getLong(0)
                val eventUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    planId
                )
                cr.query(
                    eventUri, PlannerUtils.eventProjection,
                    CalendarContract.Events.CALENDAR_ID + " = ?", arrayOf(plannerCalendarId), null
                )?.use { eventCursor ->
                    if (eventCursor.moveToFirst()) {
                        eventValues.copyEventData(eventCursor)
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
    whereArgs: Array<Any>?,
) = //https://github.com/sqlcipher/sqlcipher-android/issues/50
    update(table, SQLiteDatabase.CONFLICT_NONE, values, whereClause, whereArgs ?: emptyArray())

fun SupportSQLiteDatabase.insert(table: String, values: ContentValues): Long =
    insert(table, SQLiteDatabase.CONFLICT_NONE, values)

/**
 * insert where conflicts are ignored instead of raising exception
 */
fun SupportSQLiteDatabase.safeInsert(table: String, values: ContentValues): Long =
    insert(table, SQLiteDatabase.CONFLICT_IGNORE, values)


fun SupportSQLiteDatabase.query(
    table: String,
    columns: Array<String>?,
    selection: String? = null,
    selectionArgs: Array<Any>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null,
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

fun SupportSQLiteDatabase.dualQuery(
    columns: Array<String>,
): Cursor = query(SimpleSQLiteQuery("SELECT ${columns.joinToString(", ")}"))

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
    distinct: Boolean = false,
): String = subQueries.joinToString(if (distinct) " UNION " else " UNION ALL ") +
        (if (sortOrder != null) " ORDER BY $sortOrder" else "") +
        if (limit != null) " LIMIT $limit" else ""

fun computeWhere(vararg parts: CharSequence?) = parts
    .mapNotNull { part -> part?.takeIf { it.isNotEmpty() }?.let { "($it)" } }
    .takeIf { it.isNotEmpty() }
    ?.joinToString(" AND ")

fun backup(
    backupDir: File,
    context: Context,
    prefHandler: PrefHandler,
    lenientMode: Boolean,
): Result<Unit> {
    try {
        cacheEventData(context, prefHandler)
        cacheSyncState(context)
    } catch (e: Exception) {
        if (!lenientMode) throw e
    }
    return with(context.contentResolver.acquireContentProviderClient(TransactionProvider.AUTHORITY)!!) {
        try {
            (localContentProvider as BaseTransactionProvider).backup(
                context,
                backupDir,
                lenientMode
            )
        } finally {
            release()
        }
    }
}

fun checkSyncAccounts(context: Context) {
    val validAccounts = getAccountNames(context)
    val where =
        if (validAccounts.isNotEmpty())
            "$KEY_SYNC_ACCOUNT_NAME NOT " + Operation.IN.getOp(validAccounts.size)
        else null
    context.contentResolver.update(
        TransactionProvider.ACCOUNTS_URI, ContentValues(1).apply {
            putNull(KEY_SYNC_ACCOUNT_NAME)
        },
        where, validAccounts
    )
}

fun insertEventAndUpdatePlan(
    contentResolver: ContentResolver,
    eventValues: ContentValues,
    templateId: Long,
): Boolean {
    val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
    val planId = ContentUris.parseId(uri!!)
    Timber.i("event copied with new id %d ", planId)
    val planValues = ContentValues()
    planValues.put(KEY_PLANID, planId)
    val updated = contentResolver.update(
        ContentUris.withAppendedId(Template.CONTENT_URI, templateId),
        planValues, null, null
    )
    return updated > 0
}

fun Cursor.calculateEquivalentAmount(homeCurrency: CurrencyUnit, baseAmount: Money) =
    getLongOrNull(KEY_EQUIVALENT_AMOUNT)?.let { Money(homeCurrency, it) } ?: Money(
        homeCurrency, baseAmount.amountMajor.multiply(
            calculateRealExchangeRate(
                getDouble(KEY_EXCHANGE_RATE),
                baseAmount.currencyUnit, homeCurrency
            )
        )
    )

fun SupportSQLiteDatabase.uuidForTransaction(id: Long): String = query(
    table = TABLE_TRANSACTIONS,
    columns = arrayOf(KEY_UUID),
    selection = "$KEY_ROWID = ?",
    selectionArgs = arrayOf(id)
).use {
    it.moveToFirst()
    it.getString(0)
}

fun SupportSQLiteDatabase.findTransactionByUuid(uuid: String) = query(
    table = TABLE_TRANSACTIONS,
    columns = arrayOf(KEY_ROWID),
    selection = "$KEY_UUID = ?",
    selectionArgs = arrayOf(uuid)
).use {
    it.moveToFirst()
    it.getLong(0)
}

fun SupportSQLiteDatabase.getForeignKeyInfoAsString(tableName: String) =
    query("PRAGMA foreign_key_list($tableName)").useAndMapToList { cursor ->
        StringBuilder()
            .append("Foreign Key Constraint:")
            .append(" ID: ${cursor.getInt(0)};")
            .append(" Sequence: ${cursor.getInt(1)};")
            .append(" Referenced Table: ${cursor.getString(2)};")
            .append(" From Column: ${cursor.getString(3)};")
            .append(" To Column: ${cursor.getString(4)};")
            .append(" On Update: ${cursor.getString(5)};")
            .append(" On Delete: ${cursor.getString(6)}.")
            .toString()
    }.joinToString("\n")

fun SupportSQLiteDatabase.getForeignKeyCheckInfoAsString(tableName: String) =
    query("PRAGMA foreign_key_check($tableName)").useAndMapToList { cursor ->
        StringBuilder()
            .append("Foreign Key Violation:")
            .append(" Table: ${cursor.getString(0)};")
            .append(" Row ID: ${cursor.getLong(1)};")
            .append(" Parent Table: ${cursor.getString(2)};")
            .append(" Foreign key id: ${cursor.getLong(3)};")
            .toString()
    }.joinToString("\n")
