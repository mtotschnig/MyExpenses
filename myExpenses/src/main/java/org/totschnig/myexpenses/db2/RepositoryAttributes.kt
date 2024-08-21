package org.totschnig.myexpenses.db2

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTRIBUTE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CONTEXT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ATTRIBUTES
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_ATTRIBUTES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_ATTRIBUTES_URI
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.insert
import org.totschnig.myexpenses.provider.useAndMapToList
import java.util.EnumSet

interface Attribute {
    val name: String
    val context: String
    val userVisible: Boolean

    companion object {
        fun from(cursor: Cursor): Pair<Attribute, String> {
            val attribute = cursor.getString(KEY_ATTRIBUTE_NAME)
            return when (val context = cursor.getString(KEY_CONTEXT)) {
                FinTsAttribute.CONTEXT -> FinTsAttribute.valueOf(attribute)
                BankingAttribute.CONTEXT -> BankingAttribute.valueOf(attribute)
                else -> throw IllegalStateException("Unknown context $context")
            } to cursor.getString(KEY_VALUE)
        }

        fun <E> initDatabase(
            db: SupportSQLiteDatabase,
            attributeClass: Class<E>
        ) where E : Enum<E>, E : Attribute {
            initDatabaseInternal(db, attributeClass, TABLE_ATTRIBUTES, KEY_ATTRIBUTE_NAME, KEY_CONTEXT)
        }

        fun <E> initDatabaseInternal(
            db: SupportSQLiteDatabase,
            attributeClass: Class<E>,
            table: String,
            keyName: String,
            keyContext: String
        ) where E : Enum<E>, E : Attribute {
            EnumSet.allOf(attributeClass).forEach {
                db.insert(table, ContentValues().apply {
                    put(keyName, it.name)
                    put(keyContext, it.context)
                })
            }
        }
    }
}

/**
 * see VerwendungszweckUtil.Tag
 */
enum class FinTsAttribute(override val userVisible: Boolean = true) : Attribute {
    EREF,
    KREF,
    MREF,
    CRED,
    DBET,
    SALDO,
    CHECKSUM(false)
    ;

    companion object {
        const val CONTEXT = "FinTS"
    }

    override val context: String
        get() = CONTEXT
}

enum class BankingAttribute : Attribute {
    NAME, BLZ, IBAN, BIC, NUMBER, SUBNUMBER, LAST_SYCNED_WITH_BANK;

    companion object {
        const val CONTEXT = "Banking"
    }

    override val userVisible = true
    override val context: String
        get() = CONTEXT

}

fun Repository.saveTransactionAttributes(transactionId: Long, attributes: Map<out Attribute, String>) {
    saveAttributes(TRANSACTIONS_ATTRIBUTES_URI, KEY_TRANSACTIONID, transactionId, attributes)
}

fun Repository.saveAccountAttributes(accountId: Long, attributes: Map<out Attribute, String>) {
    saveAttributes(ACCOUNTS_ATTRIBUTES_URI, KEY_ACCOUNTID, accountId, attributes)
}

private fun Repository.saveAttributes(uri: Uri, linkColumn: String, rowId: Long, attributes: Map<out Attribute, String>) {
    val ops = ArrayList<ContentProviderOperation>()
    attributes.forEach {
        ops.add(
            ContentProviderOperation.newInsert(uri)
                .withValue(linkColumn, rowId)
                .withValue(KEY_ATTRIBUTE_NAME, it.key.name)
                .withValue(KEY_CONTEXT, it.key.context)
                .withValue(KEY_VALUE, it.value)
                .build()
        )
    }
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
}

@SuppressLint("Recycle")
fun Repository.loadAttributes(transactionId: Long): List<Pair<Attribute, String>> {
    return contentResolver.query(
        TRANSACTIONS_ATTRIBUTES_URI, null,
        "$KEY_TRANSACTIONID = ?", arrayOf(transactionId.toString()), null
    )?.useAndMapToList { Attribute.from(it) } ?: emptyList()
}