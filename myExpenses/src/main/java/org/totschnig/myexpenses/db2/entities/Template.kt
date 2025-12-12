package org.totschnig.myexpenses.db2.entities

import android.database.Cursor
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_ICON
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_COMMENT
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.KEY_DEFAULT_ACTION
import org.totschnig.myexpenses.provider.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.KEY_METHODID
import org.totschnig.myexpenses.provider.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PATH
import org.totschnig.myexpenses.provider.KEY_PAYEEID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.KEY_PLANID
import org.totschnig.myexpenses.provider.KEY_PLAN_EXECUTION
import org.totschnig.myexpenses.provider.KEY_PLAN_EXECUTION_ADVANCE
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_SEALED
import org.totschnig.myexpenses.provider.KEY_TITLE
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT_CURRENCY
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.TextUtils

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
    val uuid: String,
    val parentId: Long? = null, // For SPLIT children
    val planId: Long? = null,
    val planExecutionAutomatic: Boolean = false,
    val planExecutionAdvance: Int = 0,
    val defaultAction: Action = Action.SAVE,
    val originalAmount: Long? = null,
    val originalCurrency: String? = null,
    val debtId: Long? = null,
    val dynamic: Boolean = false,
    /** Read-only property holding the full category path, populated from a provider query. */
    val categoryPath: String? = null,
    val categoryIcon: String? = null,
    val sealed: Boolean = false,
    val currency: String? = null,
    val payeeName: String? = null,
    val methodLabel: String? = null,
    val transferAccountCurrency: String? = null,
    /** the list of linked tag ids. not loaded from DB, but populated from TransactionEditData for storage*/
    val tagList: List<Long> = emptyList(),
)  {

    val isTransfer: Boolean = transferAccountId != null

    val isSplit: Boolean = categoryId == SPLIT_CATID

    val isSplitPart: Boolean = parentId != null

    fun instantiate(uuid: String) = Transaction(
        accountId = accountId,
        amount = amount,
        transferAccountId = transferAccountId,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        methodId = methodId,
        categoryId = categoryId,
        categoryIcon = categoryIcon,
        debtId = debtId,
        comment = comment,
        payeeId = payeeId,
        categoryPath = categoryPath,
        currency = currency,
        payeeName = payeeName,
        methodLabel = methodLabel,
        uuid = uuid,
        tagList = tagList
    )

    companion object {
        fun deriveFrom(transaction: Transaction, title: String) = with(transaction) {
            Template(
                title = title,
                amount = amount,
                accountId = accountId,
                comment = comment,
                categoryId = categoryId,
                categoryIcon = categoryIcon,
                categoryPath = categoryPath,
                payeeId = payeeId,
                payeeName = payeeName,
                methodId = methodId,
                methodLabel = methodLabel,
                transferAccountId = transferAccountId,
                currency = currency,
                tagList = transaction.tagList,
                debtId = debtId,
                uuid = generateUuid()
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
                methodLabel = getStringOrNull(KEY_METHOD_LABEL),
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
                categoryIcon = getStringOrNull(KEY_ICON),
                sealed = getBoolean(KEY_SEALED),
                currency = getStringOrNull(KEY_CURRENCY),
                payeeName = getStringOrNull(KEY_PAYEE_NAME),
                transferAccountCurrency = getStringOrNull(KEY_TRANSFER_ACCOUNT_CURRENCY)
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
