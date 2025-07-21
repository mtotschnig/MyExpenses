package org.totschnig.myexpenses.provider

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import org.totschnig.myexpenses.dialog.ArchiveInfo
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVED
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNT_TYPES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CHANGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.toEndOfDayEpoch
import org.totschnig.myexpenses.util.toStartOfDayEpoch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun subSelectTemplate(colum: String) =
    "(SELECT %1\$s FROM $TABLE_TRANSACTIONS WHERE $KEY_UUID = ?)".format(Locale.ROOT, colum)

fun SupportSQLiteDatabase.unarchive(
    values: ContentValues,
    callerIsNotSyncAdapter: Boolean,
): Int {
    val uuid =
        values.getAsString(KEY_UUID) ?: uuidForTransaction(values.getAsLong(KEY_ROWID))
    val rowIdSubSelect = subSelectTemplate(KEY_ROWID)
    val accountIdSubSelect = subSelectTemplate(KEY_ACCOUNTID)

    return safeUpdateWithSealed {
        TransactionProvider.pauseChangeTrigger(this)
        //parts are promoted to independence
        execSQL(
            "UPDATE $TABLE_TRANSACTIONS SET $KEY_PARENTID = null, $KEY_STATUS = $STATUS_NONE WHERE $KEY_PARENTID = $rowIdSubSelect ",
            arrayOf(uuid)
        )
        //Change is recorded
        if (callerIsNotSyncAdapter) {
            execSQL(
                """INSERT INTO $TABLE_CHANGES
                            | ($KEY_TYPE, $KEY_ACCOUNTID, $KEY_SYNC_SEQUENCE_LOCAL, $KEY_UUID)
                            | SELECT '${TransactionChange.Type.unarchive.name}', $KEY_ROWID, $KEY_SYNC_SEQUENCE_LOCAL, ?
                            | FROM $TABLE_ACCOUNTS
                            | WHERE $KEY_ROWID = $accountIdSubSelect AND $KEY_SYNC_ACCOUNT_NAME IS NOT NULL""".trimMargin(),
                arrayOf(uuid, uuid)
            )
        }
        //parent is deleted
        val count = delete(TABLE_TRANSACTIONS, "$KEY_UUID = ?", arrayOf(uuid))
        TransactionProvider.resumeChangeTrigger(this)
        count
    }
}

private const val ARCHIVE_SELECTION =
    "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null AND $KEY_STATUS != $STATUS_UNCOMMITTED AND $KEY_DATE >= ? AND $KEY_DATE <= ?"

/**
 * @return Cursor of statistics for archive grouped by type:
 * if @param onlyCheck == true: 0 - status, 1 - number of nested archives, 2 - number of transactions
 * if @param onlyCheck == false: 0 - status, 1 - number of nested archives, 2 - amount sum, 3 - date of last transaction
 */
private fun SupportSQLiteDatabase.archiveInfo(
    accountId: Long,
    start: LocalDate,
    end: LocalDate,
    onlyCheck: Boolean,
) = query(
    table = TABLE_TRANSACTIONS,
    columns = arrayOf(
        KEY_CR_STATUS,
        "sum($KEY_STATUS = $STATUS_ARCHIVE)",
    ) + if (onlyCheck) arrayOf("count(*)") else arrayOf(
        "sum($KEY_AMOUNT)",
        "max($KEY_DATE)"
    ),
    selection = ARCHIVE_SELECTION,
    selectionArgs = arrayOf(
        accountId,
        start.toStartOfDayEpoch(),
        end.toEndOfDayEpoch()
    ),
    groupBy = KEY_CR_STATUS
)

private fun Cursor.hasNested() = getInt(1) > 0

private fun Bundle.parseArchiveArguments() = Triple(
    getLong(KEY_ACCOUNTID),
    BundleCompat.getSerializable(this, KEY_START, LocalDate::class.java)!!,
    BundleCompat.getSerializable(this, KEY_END, LocalDate::class.java)!!
)

private fun SupportSQLiteDatabase.accountType(accountId: Long) =
    query(TABLE_ACCOUNT_TYPES, null, "$KEY_ROWID = (SELECT $KEY_TYPE FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = ?)", arrayOf(accountId)).use {
        it.moveToFirst()
        AccountType.fromCursor(it)
    }

fun SupportSQLiteDatabase.archive(extras: Bundle): Long {
    val (accountId, start, end) = extras.parseArchiveArguments()

    val (crStatus, archiveSum, archiveDate) = archiveInfo(
        accountId,
        start,
        end,
        false
    ).use { cursor ->
        when (cursor.count) {
            0 -> throw IllegalStateException("No transactions to archive.")
            1 -> {
                cursor.moveToFirst()
                if (cursor.hasNested()) throw IllegalStateException("Nested archive is not supported.")
                Triple(cursor.getString(0), cursor.getLong(2), cursor.getLong(3))
            }

            else -> {
                val states = cursor.useAndMapToMap {
                    it.getString(0) to Triple(
                        it.hasNested(),
                        it.getLong(2),
                        it.getLong(3)
                    )
                }
                if (states.any { it.value.first }) {
                    throw IllegalStateException("Nested archive is not supported.")
                }

                if (states.keys.filter { it != CrStatus.VOID.name }.size > 1 &&
                    accountType(accountId).supportsReconciliation
                ) {
                    throw IllegalStateException("Transactions in archive have different states.")
                }
                val nonVoidStates = states.filter { it.key != CrStatus.VOID.name }.values
                //If we archive a cash account, where we find multiple states (which can happen when user updates account type),
                // we create an unreconciled transaction, since this is the expected state for cash accounts
                Triple(
                    CrStatus.UNRECONCILED.name,
                    nonVoidStates.sumOf { it.second },
                    nonVoidStates.maxOf { it.third })
            }
        }
    }

    return safeUpdateWithSealed {
        val archiveId = insert(TABLE_TRANSACTIONS, ContentValues().apply {
            put(KEY_ACCOUNTID, accountId)
            put(KEY_DATE, archiveDate)
            put(KEY_VALUE_DATE, archiveDate)
            put(KEY_AMOUNT, archiveSum)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            put(KEY_COMMENT, start.format(formatter) + " - " + end.format(formatter))
            put(KEY_STATUS, STATUS_ARCHIVE)
            put(KEY_CR_STATUS, crStatus)
            put(KEY_UUID, Model.generateUuid())
        })
        update(
            table = TABLE_TRANSACTIONS,
            values = ContentValues().apply {
                put(KEY_PARENTID, archiveId)
                put(KEY_STATUS, STATUS_ARCHIVED)
            },
            whereClause = "$ARCHIVE_SELECTION AND $KEY_ROWID != ?",
            whereArgs = arrayOf(
                accountId,
                start.toStartOfDayEpoch(),
                end.toEndOfDayEpoch(),
                archiveId
            )
        )
        archiveId
    }
}

fun SupportSQLiteDatabase.canBeArchived(extras: Bundle): ArchiveInfo {
    val (accountId, start, end) = extras.parseArchiveArguments()
    val accountType = accountType(accountId)
    val empty = ArchiveInfo(
        count = 0,
        hasNested = false,
        statuses = emptyList(),
        accountType = accountType
    )
    return archiveInfo(accountId, start, end, true).useAndReduce(empty) { acc, cursor ->
        ArchiveInfo(
            count = acc.count + cursor.getInt(2),
            hasNested = acc.hasNested || cursor.hasNested(),
            statuses = acc.statuses + enumValueOf<CrStatus>(cursor.getString(0)),
            accountType = accountType
        )
    }
}