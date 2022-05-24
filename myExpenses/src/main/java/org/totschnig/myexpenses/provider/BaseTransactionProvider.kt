package org.totschnig.myexpenses.provider

import android.content.ContentProvider
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.FileCopyUtils
import timber.log.Timber
import java.io.File
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
    lateinit var prefHandler: PrefHandler

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

        fun categoryBudgetJoin(joinType: String) =
            " $joinType JOIN $TABLE_BUDGET_CATEGORIES ON ($KEY_CATID = $TREE_CATEGORIES.$KEY_ROWID AND $TABLE_BUDGET_CATEGORIES.$KEY_BUDGETID = ?)"

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
                        CrashHandler.report(message)
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

    /**
     * @return number of corrupted entries
     */
    fun checkCorruptedData987() = Bundle(1).apply {
        putInt(KEY_RESULT, transactionDatabase.readableDatabase.rawQuery(
            "select count(distinct transactions.parent_id) from transactions left join transactions parent on transactions.parent_id = parent._id where transactions.parent_id is not null and parent.account_id != transactions.account_id",
            null
        ).use {
            it.moveToFirst()
            it.getInt(0)
        })
    }

    fun repairCorruptedData987(db: SQLiteDatabase) = Bundle(1).apply {
        putInt(KEY_RESULT, with (transactionDatabase.writableDatabase) {
            beginTransaction()
            try {
                execSQL("update transactions set account_id = (select account_id from transactions children where children.parent_id = transactions._id) where account_id != (select account_id from transactions children where children.parent_id = transactions._id)")
                val result = db.compileStatement("SELECT changes()").simpleQueryForLong().toInt()
                setTransactionSuccessful()
                result
            } finally {
                endTransaction()
            }
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
}