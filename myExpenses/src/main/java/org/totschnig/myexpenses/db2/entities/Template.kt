package org.totschnig.myexpenses.db2.entities

import android.content.ContentValues
import android.database.Cursor
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_EXECUTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_EXECUTION_ADVANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.TextUtils
import java.util.UUID

/**
 * A standalone data class representing a record in the `templates` table.
 * It is a blueprint for creating future transactions, transfers, or splits.
 */
data class Template(
    val title: String,
    val accountId: Long,
    val id: Long = 0,
    val amount: Long = 0L,
    val comment: String? = null,
    val categoryId: Long? = null,
    val payeeId: Long? = null,
    val methodId: Long? = null,
    val transferAccountId: Long? = null, // For TRANSFER type
    val uuid: String? = null,
    val parentId: Long? = null, // For SPLIT children
    val planId: Long? = null,
    val planExecutionAutomatic: Boolean = false,
    val planExecutionAdvance: Int = 0,
    val defaultAction: Action = Action.EDIT,
    val originalAmount: Long? = null,
    val originalCurrency: String? = null,
    val debtId: Long? = null,
    val dynamic: Boolean = false,
    /** Read-only property holding the full category path, populated from a provider query. */
    val categoryPath: String? = null,
    val sealed: Boolean = false,
    val currency: String? = null,
    val payeeName: String? = null,
)  {

    fun asContentValues(): ContentValues {
        return ContentValues().apply {
            put(KEY_TITLE, title)
            put(KEY_AMOUNT, amount)
            // Storing currency is not in TEMPLATE_CREATE, but it is in `fromCursor`.
            // We assume currency comes from the account.
            put(KEY_COMMENT, comment)
            put(KEY_ACCOUNTID, accountId)
            put(KEY_CATID, categoryId)
            put(KEY_PAYEEID, payeeId)
            put(KEY_METHODID, methodId)
            put(KEY_TRANSFER_ACCOUNT, transferAccountId)
            put(KEY_PARENTID, parentId)
            put(KEY_UUID, uuid)
            put(KEY_PLANID, planId)
            put(KEY_PLAN_EXECUTION, if (planExecutionAutomatic) 1 else 0)
            put(KEY_PLAN_EXECUTION_ADVANCE, planExecutionAdvance)
            put(KEY_DEFAULT_ACTION, defaultAction.name)
            put(KEY_ORIGINAL_AMOUNT, originalAmount)
            put(KEY_ORIGINAL_CURRENCY, originalCurrency)
            put(KEY_DEBT_ID, debtId)
        }
    }

    fun compileDescription(app: MyApplication): String {
        return "TODO(Not yet implemented)"
    }

    val isTransfer: Boolean = transferAccountId != null

    val isSplit: Boolean = categoryId == DatabaseConstants.SPLIT_CATID

    val isSplitPart: Boolean = parentId != null

    fun instantiate() = Transaction(
        accountId = accountId,
        amount = amount,
        transferAccountId = transferAccountId,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        methodId = methodId,
        categoryId = categoryId,
        debtId = debtId,
        comment = comment,
        payeeId = payeeId,
        categoryPath = categoryPath,
        currency = currency,
        uuid = Model.generateUuid()
    )

    companion object {
        fun deriveFrom(transaction: Transaction, title: String) = with(transaction) {
            Template(
                title = title,
                amount = amount,
                accountId = accountId,
                comment = comment,
                categoryId = categoryId,
                payeeId = payeeId,
                methodId = methodId,
                transferAccountId = transferAccountId,
                currency = currency
            )
        }
        fun fromCursor(cursor: Cursor) = with(cursor) {
            Template(
                id = getLong(KEY_ROWID),
                title = getString(KEY_TITLE),
                amount = getLong(KEY_AMOUNT),
                accountId = getLong(KEY_ACCOUNTID),
                comment = getString(KEY_COMMENT),
                categoryId = getLongOrNull(KEY_CATID),
                payeeId = getLongOrNull(KEY_PAYEEID),
                methodId = getLongOrNull(KEY_METHODID),
                transferAccountId = getLongOrNull(KEY_TRANSFER_ACCOUNT),
                uuid = getString(KEY_UUID),
                parentId = getLongOrNull(KEY_PARENTID),
                planId = getLongOrNull(KEY_PLANID),
                planExecutionAutomatic = getInt(KEY_PLAN_EXECUTION) > 0,
                planExecutionAdvance = getInt(KEY_PLAN_EXECUTION_ADVANCE),
                defaultAction = getEnum(KEY_DEFAULT_ACTION, Action.EDIT),
                originalAmount = getLongOrNull(KEY_ORIGINAL_AMOUNT),
                originalCurrency = getStringOrNull(KEY_ORIGINAL_CURRENCY),
                debtId = getLongOrNull(KEY_DEBT_ID),
                dynamic = getBoolean(KEY_DYNAMIC),
                categoryPath = getStringOrNull(KEY_PATH),
                sealed = getBoolean(KEY_SEALED),
                currency = getStringOrNull(KEY_CURRENCY),
                payeeName = getStringOrNull(KEY_PAYEE_NAME)
            )
        }
    }

    /**
     * Defines the default action to perform when a template is selected.
     * Based on the table's CHECK constraint.
     */
    enum class Action {
        SAVE, EDIT;

        companion object {
            @JvmField
            val JOIN: String = TextUtils.joinEnum(Action::class.java)
        }
    }
}
