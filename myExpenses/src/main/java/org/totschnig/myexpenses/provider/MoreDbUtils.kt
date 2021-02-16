package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TPYE_LIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTTYES_METHODS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS

fun safeUpdateWithSealedAccounts(db: SQLiteDatabase, runnable: Runnable) {
    db.beginTransaction()
    try {
        ContentValues(1).apply {
            put(DatabaseConstants.KEY_SEALED, -1)
            db.update(DatabaseConstants.TABLE_ACCOUNTS, this, DatabaseConstants.KEY_SEALED + "= ?", arrayOf("1"))
        }
        runnable.run()
        ContentValues(1).apply {
            put(DatabaseConstants.KEY_SEALED, 1)
            db.update(DatabaseConstants.TABLE_ACCOUNTS, this, DatabaseConstants.KEY_SEALED + "= ?", arrayOf("-1"))
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

fun groupByForPaymentMethodQuery(projection: Array<String>) =
        if (projection.contains(KEY_ACCOUNT_TPYE_LIST)) KEY_ROWID else null

fun tableForPaymentMethodQuery(projection: Array<String>) =
        if (projection.contains(KEY_ACCOUNT_TPYE_LIST))
            "${TABLE_METHODS} left join $TABLE_ACCOUNTTYES_METHODS on $KEY_METHODID = $KEY_ROWID"
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