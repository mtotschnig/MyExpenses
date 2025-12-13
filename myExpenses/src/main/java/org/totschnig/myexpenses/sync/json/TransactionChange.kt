package org.totschnig.myexpenses.sync.json

import android.database.Cursor
import kotlinx.serialization.Serializable
import org.totschnig.myexpenses.model2.CategoryInfo
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_COMMENT
import org.totschnig.myexpenses.provider.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.KEY_METHOD_LABEL
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.KEY_PARENT_UUID
import org.totschnig.myexpenses.provider.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.KEY_STATUS
import org.totschnig.myexpenses.provider.KEY_TIMESTAMP
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.KEY_TYPE
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_ACCOUNT_UUID
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.TextUtils.joinEnum


@Serializable
data class TransactionChange(
    val type: Type,
    val uuid: String,
    val timeStamp: Long,
    val appInstance: String? = null,
    val parentUuid: String? = null,
    val comment: String? = null,
    val date: Long? = null,
    val valueDate: Long? = null,
    val amount: Long? = null,
    val originalAmount: Long? = null,
    val originalCurrency: String? = null,
    val equivalentAmount: Long? = null,
    val equivalentCurrency: String? = null,
    val label: String? = null,
    val payeeName: String? = null,
    val transferAccount: String? = null,
    val methodLabel: String? = null,
    val crStatus: String? = null,
    val status: Int? = null,
    val referenceNumber: String? = null,
    val pictureUri: String? = null, // Legacy from pre 3.6.5
    val tags: Set<String>? = null, // Legacy
    val tagsV2: Set<TagInfo>? = null,
    val attachments: Set<String>? = null,
    val splitParts: List<TransactionChange>? = null,
    val categoryInfo: List<CategoryInfo>? = null,
) {

    val isCreate: Boolean
        get() = type == Type.created

    val isUpdate: Boolean
        get() = type == Type.updated

    val isCreateOrUpdate: Boolean
        get() = isCreate || isUpdate

    val isDelete: Boolean
        get() = type == Type.deleted

    val isCUD: Boolean
        get() = isCreate || isUpdate || isDelete


    val isEmpty: Boolean
        get() = isCreateOrUpdate && comment == null && date == null && amount == null &&
            label == null && payeeName == null && transferAccount == null && methodLabel == null &&
            crStatus == null && referenceNumber == null && pictureUri == null && splitParts == null &&
            originalAmount == null && (equivalentAmount == null || equivalentAmount == 0L) &&
            parentUuid == null && tags == null && tagsV2 == null && categoryInfo == null &&
            attachments == null && status == null

    enum class Type {
        created, updated, deleted, unsplit, metadata, link, tags, attachments, unarchive;

        companion object {
            @JvmField
            val JOIN: String = joinEnum(Type::class.java)
        }

    }

    fun withCurrentTimeStamp() = copy(timeStamp = timeStamp)

    companion object {

        val currentTimStamp = System.currentTimeMillis() / 1000

        fun fromCursor(cursor: Cursor): TransactionChange {
            return TransactionChange(
                type = cursor.getEnum(KEY_TYPE, Type.created),
                uuid = cursor.getString(KEY_UUID),
                timeStamp = cursor.getLong(KEY_TIMESTAMP),
                parentUuid = cursor.getStringOrNull(KEY_PARENT_UUID),
                comment = cursor.getStringOrNull(KEY_COMMENT),
                date = cursor.getLongOrNull(KEY_DATE),
                valueDate = cursor.getLongOrNull(KEY_VALUE_DATE),
                amount = cursor.getLongOrNull(KEY_AMOUNT),
                originalAmount = cursor.getLongOrNull(KEY_ORIGINAL_AMOUNT),
                originalCurrency = cursor.getStringOrNull(KEY_ORIGINAL_CURRENCY),
                equivalentAmount = cursor.getLongOrNull(KEY_EQUIVALENT_AMOUNT),
                payeeName = cursor.getStringOrNull(KEY_PAYEE_NAME),
                transferAccount = cursor.getStringOrNull(KEY_TRANSFER_ACCOUNT),
                methodLabel = cursor.getStringOrNull(KEY_METHOD_LABEL),
                crStatus = cursor.getStringOrNull(KEY_CR_STATUS),
                status = cursor.getIntOrNull(KEY_STATUS),
                referenceNumber = cursor.getStringOrNull(KEY_REFERENCE_NUMBER)
            )
        }

        val PROJECTION: Array<String> = arrayOf<String>(
            KEY_TYPE,
            KEY_UUID,
            KEY_TIMESTAMP,
            KEY_PARENT_UUID,
            "TRIM($KEY_COMMENT) AS $KEY_COMMENT",
            KEY_DATE,
            KEY_VALUE_DATE,
            KEY_AMOUNT,
            KEY_ORIGINAL_AMOUNT,
            KEY_ORIGINAL_CURRENCY,
            KEY_EQUIVALENT_AMOUNT,
            "NULLIF(TRIM($KEY_PAYEE_NAME),'') AS $KEY_PAYEE_NAME",
            TRANSFER_ACCOUNT_UUID,
            KEY_CATID,
            KEY_METHOD_LABEL,
            KEY_CR_STATUS,
            KEY_STATUS,
            "TRIM($KEY_REFERENCE_NUMBER) AS $KEY_REFERENCE_NUMBER"
        )
    }
}
