package org.totschnig.myexpenses.provider

import androidx.sqlite.db.SupportSQLiteDatabase
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENT_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_SEQUENCE_LOCAL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVED
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CHANGES
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_EQUIVALENT_AMOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.sync.json.TransactionChange

private val DELETE_TRIGGER_ACTION =
    """INSERT INTO $TABLE_CHANGES($KEY_TYPE,$KEY_SYNC_SEQUENCE_LOCAL, $KEY_ACCOUNTID,$KEY_UUID,$KEY_PARENT_UUID) 
        VALUES (
        '${TransactionChange.Type.deleted}',
        ${sequenceNumberSelect("old")},
        old.$KEY_ACCOUNTID, old.$KEY_UUID,
        ${parentUuidExpression("old")});"""

private val DELETE_TRIGGER_ACTION_AFTER_TRANSFER_UPDATE =
    """ BEGIN
        INSERT INTO $TABLE_CHANGES($KEY_TYPE,$KEY_SYNC_SEQUENCE_LOCAL, $KEY_ACCOUNTID,$KEY_UUID,$KEY_PARENT_UUID) 
        VALUES (
        '${TransactionChange.Type.deleted}',
        ${sequenceNumberSelect("old")},
        old.$KEY_ACCOUNTID,
        new.$KEY_UUID,
        ${parentUuidExpression("old")});
        END;"""

private val TRANSACTIONS_DELETE_AFTER_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER delete_after_update_change_log AFTER UPDATE ON $TABLE_TRANSACTIONS WHEN 
        ${shouldWriteChangeTemplate("old")} AND 
        old.$KEY_ACCOUNTID != new.$KEY_ACCOUNTID AND 
        new.$KEY_STATUS != $STATUS_UNCOMMITTED
        $DELETE_TRIGGER_ACTION_AFTER_TRANSFER_UPDATE"""

private val TRANSACTIONS_DELETE_TRIGGER_CREATE =
    """CREATE TRIGGER delete_change_log AFTER DELETE ON $TABLE_TRANSACTIONS 
        WHEN ${shouldWriteChangeTemplate("old")} AND 
        old.$KEY_STATUS != $STATUS_UNCOMMITTED AND 
        EXISTS (SELECT 1 FROM $TABLE_ACCOUNTS WHERE $KEY_ROWID = old.$KEY_ACCOUNTID)
        BEGIN
        $DELETE_TRIGGER_ACTION
        END;"""

/**
 * we ignore setting of exported flag
 * if account is changed, we need to delete transaction from one account, and add it to the other
 * if a new transfer is inserted, the first peer is updated, after second one is added, and we can skip this update here
 * during transfer update, uuid is temporarily set to null, we need to skip this change here, otherwise we run into SQLiteConstraintException
 */
val TRANSACTIONS_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER update_change_log AFTER UPDATE ON $TABLE_TRANSACTIONS WHEN 
        ${shouldWriteChangeTemplate("old")} AND 
        old.$KEY_STATUS != $STATUS_UNCOMMITTED AND new.$KEY_STATUS != $STATUS_UNCOMMITTED AND 
        (new.$KEY_STATUS = old.$KEY_STATUS OR new.$KEY_STATUS = $STATUS_ARCHIVED) AND
        new.$KEY_ACCOUNTID = old.$KEY_ACCOUNTID AND 
        new.$KEY_TRANSFER_PEER IS old.$KEY_TRANSFER_PEER AND 
        new.$KEY_UUID IS NOT NULL
        BEGIN INSERT INTO $TABLE_CHANGES($KEY_TYPE,$KEY_SYNC_SEQUENCE_LOCAL, $KEY_UUID, $KEY_ACCOUNTID, $KEY_PARENT_UUID, $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_ORIGINAL_AMOUNT, $KEY_ORIGINAL_CURRENCY, $KEY_CATID, $KEY_PAYEEID, $KEY_TRANSFER_ACCOUNT, $KEY_METHODID, $KEY_CR_STATUS, $KEY_STATUS, $KEY_REFERENCE_NUMBER)
        VALUES ('${TransactionChange.Type.updated}', 
        ${sequenceNumberSelect("old")},
        new.$KEY_UUID, 
        new.$KEY_ACCOUNTID,
        ${parentUuidExpression("new")},
        ${buildChangeTriggerDefinitionForTextColumn(KEY_COMMENT)}, 
        ${buildChangeTriggerDefinitionForColumnNotNull(KEY_DATE)},
        ${buildChangeTriggerDefinitionForColumnNotNull(KEY_VALUE_DATE)},
        ${buildChangeTriggerDefinitionForColumnNotNull(KEY_AMOUNT)},
        ${buildChangeTriggerDefinitionForIntegerColumn(KEY_ORIGINAL_AMOUNT)},
        ${buildChangeTriggerDefinitionForTextColumn(KEY_ORIGINAL_CURRENCY)},
        ${buildChangeTriggerDefinitionForReferenceColumn(KEY_CATID)}, 
        ${buildChangeTriggerDefinitionForReferenceColumn(KEY_PAYEEID)},
        ${buildChangeTriggerDefinitionForIntegerColumn(KEY_TRANSFER_ACCOUNT)},
        ${buildChangeTriggerDefinitionForReferenceColumn(KEY_METHODID)},
        ${buildChangeTriggerDefinitionForColumnNotNull(KEY_CR_STATUS)},
        ${buildChangeTriggerDefinitionForColumnNotNull(KEY_STATUS)},
        ${buildChangeTriggerDefinitionForTextColumn(KEY_REFERENCE_NUMBER)});
        END;"""

val INSERT_TRIGGER_ACTION =
    """INSERT INTO $TABLE_CHANGES($KEY_TYPE,$KEY_SYNC_SEQUENCE_LOCAL, $KEY_UUID, $KEY_PARENT_UUID, $KEY_COMMENT, $KEY_DATE, $KEY_VALUE_DATE, $KEY_AMOUNT, $KEY_ORIGINAL_AMOUNT, $KEY_ORIGINAL_CURRENCY, $KEY_CATID, $KEY_ACCOUNTID,$KEY_PAYEEID, $KEY_TRANSFER_ACCOUNT, $KEY_METHODID,$KEY_CR_STATUS, $KEY_STATUS, $KEY_REFERENCE_NUMBER)
        VALUES ('${TransactionChange.Type.created}',
        ${sequenceNumberSelect("new")},
        new.$KEY_UUID,
        ${parentUuidExpression("new")},
        new.$KEY_COMMENT, new.$KEY_DATE, new.$KEY_VALUE_DATE, new.$KEY_AMOUNT, new.$KEY_ORIGINAL_AMOUNT, new.$KEY_ORIGINAL_CURRENCY, new.$KEY_CATID, new.$KEY_ACCOUNTID, new.$KEY_PAYEEID, new.$KEY_TRANSFER_ACCOUNT, new.$KEY_METHODID, new.$KEY_CR_STATUS, new.$KEY_STATUS, new.$KEY_REFERENCE_NUMBER);"""

val TRANSACTIONS_UUID_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER uuid_update_change_log AFTER UPDATE ON $TABLE_TRANSACTIONS 
        WHEN ${shouldWriteChangeTemplate("new")} 
        AND old.$KEY_UUID != new.$KEY_UUID 
        AND new.$KEY_STATUS != $STATUS_UNCOMMITTED
        BEGIN
        $INSERT_TRIGGER_ACTION
        $DELETE_TRIGGER_ACTION
        END;"""

val TRANSACTIONS_INSERT_TRIGGER_CREATE =
    """CREATE TRIGGER insert_change_log AFTER INSERT ON $TABLE_TRANSACTIONS
    WHEN ${shouldWriteChangeTemplate("new")} AND new.$KEY_STATUS != $STATUS_UNCOMMITTED
    BEGIN
    $INSERT_TRIGGER_ACTION
    END
    """

private val TRANSACTIONS_INSERT_AFTER_UPDATE_TRIGGER_CREATE =
    """CREATE TRIGGER insert_after_update_change_log AFTER UPDATE ON $TABLE_TRANSACTIONS
    WHEN ${shouldWriteChangeTemplate("new")} AND 
    ((old.$KEY_STATUS = $STATUS_UNCOMMITTED AND new.$KEY_STATUS != $STATUS_UNCOMMITTED) OR 
    (old.$KEY_ACCOUNTID != new.$KEY_ACCOUNTID AND new.$KEY_STATUS != $STATUS_UNCOMMITTED))
    BEGIN
    $INSERT_TRIGGER_ACTION
    END"""

fun SupportSQLiteDatabase.createOrRefreshChangeLogTriggers() {
    execSQL("DROP TRIGGER IF EXISTS insert_change_log")
    execSQL("DROP TRIGGER IF EXISTS insert_after_update_change_log")
    execSQL("DROP TRIGGER IF EXISTS delete_after_update_change_log")
    execSQL("DROP TRIGGER IF EXISTS delete_change_log")
    execSQL("DROP TRIGGER IF EXISTS update_change_log")
    execSQL("DROP TRIGGER IF EXISTS uuid_update_change_log")

    execSQL(TRANSACTIONS_INSERT_TRIGGER_CREATE)
    execSQL(TRANSACTIONS_INSERT_AFTER_UPDATE_TRIGGER_CREATE)
    execSQL(TRANSACTIONS_DELETE_AFTER_UPDATE_TRIGGER_CREATE)
    execSQL(TRANSACTIONS_DELETE_TRIGGER_CREATE)
    execSQL(TRANSACTIONS_UPDATE_TRIGGER_CREATE)
    execSQL(TRANSACTIONS_UUID_UPDATE_TRIGGER_CREATE)
}

fun SupportSQLiteDatabase.createOrRefreshEquivalentAmountTriggers() {
    execSQL("DROP TRIGGER IF EXISTS insert_equivalent_amount")
    execSQL("DROP TRIGGER IF EXISTS update_equivalent_amount")

    //KEY_STATUS Is set to 0 by default, so we explicitly set it to null
    execSQL(
        """
CREATE TRIGGER insert_equivalent_amount AFTER INSERT ON $TABLE_EQUIVALENT_AMOUNTS
    WHEN ${shouldWriteChangeTemplate("new", TABLE_EQUIVALENT_AMOUNTS)}
        BEGIN INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_UUID, $KEY_ACCOUNTID, $KEY_EQUIVALENT_AMOUNT, $KEY_SYNC_SEQUENCE_LOCAL, $KEY_STATUS)
        VALUES ('${TransactionChange.Type.updated.name}', (SELECT $KEY_UUID FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = new.$KEY_TRANSACTIONID),
        (SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = new.$KEY_TRANSACTIONID),
        new.$KEY_EQUIVALENT_AMOUNT,
        ${sequenceNumberSelect("new", TABLE_EQUIVALENT_AMOUNTS)},
        null); END
""")

    execSQL(
        """
CREATE TRIGGER update_equivalent_amount AFTER UPDATE ON $TABLE_EQUIVALENT_AMOUNTS
    WHEN ${shouldWriteChangeTemplate("old", TABLE_EQUIVALENT_AMOUNTS)}
            AND old.$KEY_EQUIVALENT_AMOUNT != new.$KEY_EQUIVALENT_AMOUNT
        BEGIN INSERT INTO $TABLE_CHANGES ($KEY_TYPE, $KEY_UUID, $KEY_ACCOUNTID, $KEY_EQUIVALENT_AMOUNT, $KEY_SYNC_SEQUENCE_LOCAL, $KEY_STATUS)
        VALUES ('${TransactionChange.Type.updated.name}', (SELECT $KEY_UUID FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = old.$KEY_TRANSACTIONID),
        (SELECT $KEY_ACCOUNTID FROM $TABLE_TRANSACTIONS WHERE $KEY_ROWID = old.$KEY_TRANSACTIONID),
        new.$KEY_EQUIVALENT_AMOUNT,
        ${sequenceNumberSelect("old", TABLE_EQUIVALENT_AMOUNTS)},
        null); END
""")
}