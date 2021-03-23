package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.sync.json.TransactionChange

fun safeUpdateWithSealedAccounts(db: SQLiteDatabase, runnable: Runnable) {
    db.beginTransaction()
    try {
        ContentValues(1).apply {
            put(DatabaseConstants.KEY_SEALED, -1)
            db.update(TABLE_ACCOUNTS, this, DatabaseConstants.KEY_SEALED + "= ?", arrayOf("1"))
        }
        runnable.run()
        ContentValues(1).apply {
            put(DatabaseConstants.KEY_SEALED, 1)
            db.update(TABLE_ACCOUNTS, this, DatabaseConstants.KEY_SEALED + "= ?", arrayOf("-1"))
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
        val sql = "UPDATE $TABLE_TRANSACTIONS SET $KEY_CATID = null, $KEY_PAYEEID = null, $KEY_UUID = ?," +
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
            val updateSql = "INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL, $KEY_UUID, $KEY_REFERENCE_NUMBER) " +
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